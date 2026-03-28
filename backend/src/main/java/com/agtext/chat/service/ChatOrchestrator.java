package com.agtext.chat.service;

import com.agtext.agent.service.AgentService;
import com.agtext.chat.domain.Message;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.service.KnowledgeRetrievalService;
import com.agtext.memory.service.MemoryExtractionService;
import com.agtext.memory.service.MemoryService;
import com.agtext.memory.service.MemorySettingsProperties;
import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.service.ModelService;
import com.agtext.task.service.TaskContextProperties;
import com.agtext.task.service.TaskContextService;
import com.agtext.tool.platform.service.ToolExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatOrchestrator {
  // --- 基础对话与持久化 ---
  private final ConversationService conversations;     // 会话元数据管理（创建、查询会话）
  private final MessageService messages;               // 消息持久化（存储用户输入和 AI 响应）

  // --- 核心推理与配置 ---
  private final ModelService modelService;             // LLM 抽象层（支持多供应商切换，如 OpenAI, Claude）
  private final ChatSettingsProperties settings;       // 全局聊天配置（如默认 System Prompt、历史消息上限）

  // --- 增强检索 (RAG) ---
  private final KnowledgeRetrievalService retrieval;   // 知识库检索服务（处理向量搜索与文档分片）

  // --- 个性化记忆系统 ---
  private final MemoryService memoryService;           // 长期记忆读写服务（存储用户偏好、背景信息）
  private final MemoryExtractionService memoryExtraction; // 记忆提取逻辑（从对话中异步分析并提取结构化记忆）
  private final MemorySettingsProperties memorySettings; // 记忆功能相关参数（如召回阈值、最大记忆条数）

  // --- 任务与执行上下文 ---
  private final TaskContextService taskContext;        // 任务上下文构建（当前项目状态、待办任务等实时背景）
  private final TaskContextProperties taskContextSettings; // 任务上下文注入逻辑的开关与参数配置

  // --- 外部扩展与多智能体 ---
  private final ToolExecutionService tools;            // 外部工具/函数执行引擎（如搜索、数据库操作）
  private final ObjectMapper objectMapper;             // JSON 解析器（用于处理工具输入/输出参数）
  private final AgentService agents;                   // 智能体路由（处理特定的专业化 Role-play 或子任务）

  /**
   * 构造函数：注入所有必要的服务组件
   * 采用构造器注入（Constructor Injection）确保依赖在类实例化时即不可变且已就绪
   */
  public ChatOrchestrator(
          ConversationService conversations,
          MessageService messages,
          ModelService modelService,
          ChatSettingsProperties settings,
          KnowledgeRetrievalService retrieval,
          MemoryService memoryService,
          MemoryExtractionService memoryExtraction,
          MemorySettingsProperties memorySettings,
          TaskContextService taskContext,
          TaskContextProperties taskContextSettings,
          ToolExecutionService tools,
          ObjectMapper objectMapper,
          AgentService agents) {
    this.conversations = conversations;
    this.messages = messages;
    this.modelService = modelService;
    this.settings = settings;
    this.retrieval = retrieval;
    this.memoryService = memoryService;
    this.memoryExtraction = memoryExtraction;
    this.memorySettings = memorySettings;
    this.taskContext = taskContext;
    this.taskContextSettings = taskContextSettings;
    this.tools = tools;
    this.objectMapper = objectMapper;
    this.agents = agents;
  }

  /**
   * 核心对话入口：处理用户消息、集成 RAG、记忆、工具调用及 Agent 逻辑
   */
  @Transactional // 保证消息持久化与业务逻辑的原子性
  public ChatResult sendMessage(
          Long conversationId,
          Long knowledgeBaseId,
          String userMessage,
          String provider,
          String modelName) {

    // 1. 合法性校验
    if (userMessage == null || userMessage.isBlank()) {
      throw new IllegalArgumentException("Message is required");
    }

    // 2. 会话管理：自动创建新会话或获取现有会话
    long convId;
    if (conversationId == null) {
      convId = conversations.create(defaultTitle(userMessage)).id();
    } else {
      convId = conversations.get(conversationId).id();
    }

    // 3. 记录用户原始请求到数据库
    long userMessageId = messages.create(convId, "user", userMessage, null, null, null);

    String trimmed = userMessage.trim();
    // === 特殊指令分支 A: 直接工具调用 (/tool) ===
    if (trimmed.startsWith("/tool")) {
      String[] parts = trimmed.split("\\s+", 3);
      if (parts.length < 2) {
        throw new IllegalArgumentException("Usage: /tool <toolName> <json?>");
      }
      String toolName = parts[1];
      String jsonText = parts.length >= 3 ? parts[2].trim() : "{}";
      var input = tryParseJson(jsonText);
      // 执行本地或远程工具，不经过 LLM 规划直接触发
      var r = tools.execute("user", new ToolExecutionService.ExecuteRequest(toolName, input, null));
      // 构造工具执行结果的系统格式消息并存库
      String content =
          "TOOL RESULT\n"
              + "tool="
              + toolName
              + "\n"
              + "status="
              + r.status()
              + (r.confirmationId() == null ? "" : ("\nconfirmationId=" + r.confirmationId()))
              + (r.summary() == null ? "" : ("\nsummary=" + r.summary()))
              + (r.data() == null ? "" : ("\n\n" + r.data().toPrettyString()));
      messages.create(convId, "assistant", content, "system", "tool", null);
      return new ChatResult(convId, new ModelResponse("system", "tool", content), List.of());
    }
    // === 特殊指令分支 B: 特定 Agent 角色路由 (/agent) ===
    if (trimmed.startsWith("/agent")) {
      String[] parts = trimmed.split("\\s+", 3);
      if (parts.length < 3) {
        throw new IllegalArgumentException("Usage: /agent <role|auto> <text>");
      }
      String role = parts[1];
      String text = parts[2];
      // 调用特定的 Agent 执行流
      ModelResponse r = agents.run(role, text, provider, modelName);
      messages.create(convId, "assistant", r.content(), r.provider(), r.model(), null);
      return new ChatResult(convId, r, List.of());
    }

    // 4. 获取历史上下文：倒序查询并翻转，确保顺序为 [旧 -> 新]
    List<Message> historyDesc = messages.listRecent(convId, settings.maxHistoryMessages());
    Collections.reverse(historyDesc);

    List<ChatMessage> prompt = new ArrayList<>();
    // 注入全局系统提示词 (System Prompt)
    prompt.add(ChatMessage.system(settings.systemPrompt()));

    // 5. 任务上下文注入 (Task Context)：注入当前运行任务的状态信息，辅助模型感知业务环境
    if (taskContextSettings.enabled()) {
      try {
        String ctx = taskContext.buildSystemContext(taskContextSettings.maxTasksInPrompt());
        if (ctx != null && !ctx.isBlank()) {
          prompt.add(ChatMessage.system(ctx));
        }
      } catch (RuntimeException ignored) {
        // 容错处理：上下文构建失败不应阻断基础对话
      }
    }

    // 6. 长期记忆注入 (Long-term Memory)：从记忆库检索已审核的片段
    var approvedMemories = memoryService.listApproved(memorySettings.maxApprovedMemoriesInPrompt());
    if (!approvedMemories.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("已确认的长期记忆（可用于更好地回答用户）：\n");
      // ... 遍历拼接记忆 ...
      for (var m : approvedMemories) {
        if (m.title() != null && !m.title().isBlank()) {
          sb.append("- ").append(m.title()).append("：").append(m.content()).append("\n");
        } else {
          sb.append("- ").append(m.content()).append("\n");
        }
      }
      prompt.add(ChatMessage.system(sb.toString()));
    }

    // 7. RAG 知识检索 (Retrieval Augmented Generation)
    List<Citation> citations = List.of();
    if (knowledgeBaseId != null) {
      var hits = retrieval.retrieve(knowledgeBaseId, userMessage, 5);
      if (!hits.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是知识库检索结果（请优先使用这些信息回答，并在回答中引用对应来源）：\n");
        // ... 拼接检索片段及引用源 ...
        for (int i = 0; i < hits.size(); i++) {
          var h = hits.get(i);
          sb.append(i + 1)
              .append(". ")
              .append(h.documentTitle() == null ? "" : h.documentTitle())
              .append(" ")
              .append(h.sourceUri() == null ? "" : h.sourceUri())
              .append("\n")
              .append(h.excerpt())
              .append("\n\n");
        }
        prompt.add(ChatMessage.system(sb.toString()));
        // 构建前端展示所需的引用信息 (Citations)
        citations =
            hits.stream()
                .map(
                    h ->
                        new Citation(
                            IdCodec.encode("doc_", h.documentId()),
                            h.documentTitle(),
                            h.sourceUri(),
                            IdCodec.encode("chk_", h.chunkId()),
                            h.excerpt(),
                            h.score()))
                .toList();
      }
    }
    // 8. 装载对话历史
    for (Message m : historyDesc) {
      prompt.add(new ChatMessage(m.role(), m.content()));
    }

    // 9. 调用大模型：发起真正的网络请求
    ModelResponse response = modelService.chat(provider, modelName, prompt);

    // 10. 异步/后置持久化：存储 AI 的回复
    messages.create(convId, "assistant", response.content(), response.provider(), response.model(), null);

    // 11. 记忆提取 (Memory Extraction)：基于对话内容异步分析，提取潜在的记忆点进入审核池
    for (var c : memoryExtraction.extractCandidates(userMessage, response.content())) {
      memoryService.createCandidate(c.title(), c.content(), "chat", convId, userMessageId, c.reason());
    }

    return new ChatResult(convId, response, citations);
  }

  /**
   * 自动生成会话标题：截取用户第一条消息的前 24 个字符作为默认标题
   */
  private static String defaultTitle(String userMessage) {
    String trimmed = userMessage.trim();
    if (trimmed.length() <= 24) {
      return trimmed;
    }
    return trimmed.substring(0, 24);
  }

  /**
   * JSON 解析工具：用于解析 /tool 指令后的参数字符串。解析失败则抛出非法参数异常
   */
  private JsonNode tryParseJson(String text) {
    try {
      return objectMapper.readTree(text);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON for /tool input");
    }
  }

  /**
   * 对话结果包装类：包含当前会话 ID、模型响应内容以及 RAG 检索的引用来源
   */
  public record ChatResult(long conversationId, ModelResponse response, List<Citation> citations) {}

  /**
   * RAG 引用详情：记录知识库片段的来源文档、标题、链接、具体切片内容及相关性评分
   */
  public record Citation(
          String documentId,
          String documentTitle,
          String sourceUri,
          String chunkId,
          String excerpt,
          double score) {}
}
