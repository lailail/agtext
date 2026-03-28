package com.agtext.agent.service;

import com.agtext.agent.domain.AgentRole;
import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.service.ModelService;
import com.agtext.tool.platform.service.ToolExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 核心业务逻辑层：负责 Agent 的角色分配、工具调用协调以及提示词（Prompt）封装。
 */
@Service
public class AgentService {
  private final ModelService models;          // 负责大模型推理调用
  private final ToolExecutionService tools;   // 负责外部工具（如搜索、数据库）执行
  private final ObjectMapper objectMapper;    // 处理 JSON 转换

  public AgentService(ModelService models, ToolExecutionService tools, ObjectMapper objectMapper) {
    this.models = models;
    this.tools = tools;
    this.objectMapper = objectMapper;
  }

  /**
   * 返回前端展示的可选角色列表及其静态描述。
   */
  public List<RoleInfo> roles() {
    return List.of(
            new RoleInfo(AgentRole.RETRIEVAL.name().toLowerCase(), "检索 Agent：联网搜索/抓取后总结（只读）。"),
            new RoleInfo(AgentRole.PLANNING.name().toLowerCase(), "规划 Agent：目标拆解、步骤计划（只读）。"),
            new RoleInfo(AgentRole.WRITING.name().toLowerCase(), "写作 Agent：摘要/文案/邮件（只读）。"));
  }

  /**
   * Agent 执行主入口。
   * 逻辑流：角色识别 -> 输入校验 -> 任务分发。
   */
  public ModelResponse run(
          String roleName, String input, String providerOverride, String modelOverride) {
    // 1. 识别或预测当前请求应采用的 Agent 角色
    AgentRole role = parseRole(roleName, input);

    String text = input == null ? "" : input.trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException("input is required");
    }

    // 2. 根据角色路由至具体的执行私有方法
    if (role == AgentRole.RETRIEVAL) {
      return runRetrieval(text, providerOverride, modelOverride);
    }
    if (role == AgentRole.PLANNING) {
      return runPlanning(text, providerOverride, modelOverride);
    }
    // 默认或指定为 WRITING
    return runWriting(text, providerOverride, modelOverride);
  }

  /**
   * 角色解析逻辑。支持显式指定或 "auto" 模式下的关键词启发式识别。
   */
  private AgentRole parseRole(String roleName, String input) {
    if (roleName == null || roleName.isBlank() || "auto".equalsIgnoreCase(roleName)) {
      return autoRole(input == null ? "" : input);
    }
    return switch (roleName.toLowerCase()) {
      case "retrieval", "search" -> AgentRole.RETRIEVAL;
      case "planning", "plan" -> AgentRole.PLANNING;
      case "writing", "write" -> AgentRole.WRITING;
      default -> autoRole(input == null ? "" : input);
    };
  }

  /**
   * 启发式意图识别（Heuristic Intent Recognition）。
   * 基于关键词快速匹配，避免为了简单的角色分配而产生额外的 LLM 推理开销。
   */
  private AgentRole autoRole(String input) {
    String t = input == null ? "" : input;
    if (t.contains("搜索") || t.contains("查一下") || t.contains("检索") || t.contains("链接") || t.contains("url")) {
      return AgentRole.RETRIEVAL;
    }
    if (t.contains("计划") || t.contains("拆解") || t.contains("步骤") || t.contains("里程碑")) {
      return AgentRole.PLANNING;
    }
    return AgentRole.WRITING;
  }

  /**
   * 规划逻辑：强制 LLM 输出结构化的任务拆解结果。
   */
  private ModelResponse runPlanning(String input, String provider, String model) {
    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(ChatMessage.system("""
            你是“规划 Agent”。你的任务是把用户目标拆成清晰、可执行的步骤（中文）。
            约束：
            - 不执行任何写入操作，只输出建议/步骤
            - 输出尽量结构化（列表/编号）
            """));
    prompt.add(ChatMessage.user(input));
    return models.chat(provider, model, prompt);
  }

  /**
   * 写作逻辑：纯生成式任务，无外部依赖。
   */
  private ModelResponse runWriting(String input, String provider, String model) {
    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(ChatMessage.system("""
            你是“写作 Agent”。你的任务是根据用户要求生成清晰、专业的中文文本。
            约束：不执行写入操作，只输出文本结果。
            """));
    prompt.add(ChatMessage.user(input));
    return models.chat(provider, model, prompt);
  }

  /**
   * 检索逻辑（RAG 工作流）：
   * 1. 调用外部搜索工具（web.search）获取原始数据。
   * 2. 将搜索结果注入 Prompt 上下文。
   * 3. LLM 基于上下文进行总结。
   */
  private ModelResponse runRetrieval(String input, String provider, String model) {
    // 构造工具调用请求：设置搜索词和返回数量限制
    ObjectNode searchIn = objectMapper.createObjectNode();
    searchIn.put("query", input);
    searchIn.put("limit", 5);

    // 同步执行外部搜索操作
    var search = tools.execute("agent", new ToolExecutionService.ExecuteRequest("web.search", searchIn, null));

    // 处理工具返回结果或异常情况
    String context = "";
    if ("succeeded".equalsIgnoreCase(search.status()) && search.data() != null) {
      context = search.data().toPrettyString();
    } else if (search.errorMessage() != null) {
      context = "search failed: " + search.errorMessage();
    }

    // 将搜索到的事实与用户意图结合，进行“依据事实”的总结生成
    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(ChatMessage.system("""
            你是“检索 Agent”。你会基于给定的搜索结果，给出简洁、可核对的总结。
            约束：
            - 不执行写入操作
            - 如信息不足请明确说明
            """));
    prompt.add(ChatMessage.system("搜索结果（JSON）：\n" + context));
    prompt.add(ChatMessage.user("用户问题：\n" + input));
    return models.chat(provider, model, prompt);
  }

  public record RoleInfo(String name, String description) {}
}