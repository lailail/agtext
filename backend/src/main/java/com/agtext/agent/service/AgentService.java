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

@Service
public class AgentService {
  private final ModelService models;
  private final ToolExecutionService tools;
  private final ObjectMapper objectMapper;

  public AgentService(ModelService models, ToolExecutionService tools, ObjectMapper objectMapper) {
    this.models = models;
    this.tools = tools;
    this.objectMapper = objectMapper;
  }

  public List<RoleInfo> roles() {
    return List.of(
        new RoleInfo(AgentRole.RETRIEVAL.name().toLowerCase(), "检索 Agent：联网搜索/抓取后总结（只读）。"),
        new RoleInfo(AgentRole.PLANNING.name().toLowerCase(), "规划 Agent：目标拆解、步骤计划（只读）。"),
        new RoleInfo(AgentRole.WRITING.name().toLowerCase(), "写作 Agent：摘要/文案/邮件（只读）。"));
  }

  public ModelResponse run(
      String roleName, String input, String providerOverride, String modelOverride) {
    AgentRole role = parseRole(roleName, input);
    String text = input == null ? "" : input.trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException("input is required");
    }

    if (role == AgentRole.RETRIEVAL) {
      return runRetrieval(text, providerOverride, modelOverride);
    }
    if (role == AgentRole.PLANNING) {
      return runPlanning(text, providerOverride, modelOverride);
    }
    if (role == AgentRole.WRITING) {
      return runWriting(text, providerOverride, modelOverride);
    }
    return runWriting(text, providerOverride, modelOverride);
  }

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

  private AgentRole autoRole(String input) {
    String t = input == null ? "" : input;
    if (t.contains("搜索")
        || t.contains("查一下")
        || t.contains("检索")
        || t.contains("链接")
        || t.contains("url")) {
      return AgentRole.RETRIEVAL;
    }
    if (t.contains("计划") || t.contains("拆解") || t.contains("步骤") || t.contains("里程碑")) {
      return AgentRole.PLANNING;
    }
    return AgentRole.WRITING;
  }

  private ModelResponse runPlanning(String input, String provider, String model) {
    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(
        ChatMessage.system(
            """
            你是“规划 Agent”。你的任务是把用户目标拆成清晰、可执行的步骤（中文）。
            约束：
            - 不执行任何写入操作，只输出建议/步骤
            - 输出尽量结构化（列表/编号）
            """));
    prompt.add(ChatMessage.user(input));
    return models.chat(provider, model, prompt);
  }

  private ModelResponse runWriting(String input, String provider, String model) {
    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(
        ChatMessage.system(
            """
            你是“写作 Agent”。你的任务是根据用户要求生成清晰、专业的中文文本。
            约束：不执行写入操作，只输出文本结果。
            """));
    prompt.add(ChatMessage.user(input));
    return models.chat(provider, model, prompt);
  }

  private ModelResponse runRetrieval(String input, String provider, String model) {
    ObjectNode searchIn = objectMapper.createObjectNode();
    searchIn.put("query", input);
    searchIn.put("limit", 5);
    var search =
        tools.execute(
            "agent", new ToolExecutionService.ExecuteRequest("web.search", searchIn, null));

    String context = "";
    if ("succeeded".equalsIgnoreCase(search.status()) && search.data() != null) {
      context = search.data().toPrettyString();
    } else if (search.errorMessage() != null) {
      context = "search failed: " + search.errorMessage();
    }

    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(
        ChatMessage.system(
            """
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
