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
  private final ConversationService conversations;
  private final MessageService messages;
  private final ModelService modelService;
  private final ChatSettingsProperties settings;
  private final KnowledgeRetrievalService retrieval;
  private final MemoryService memoryService;
  private final MemoryExtractionService memoryExtraction;
  private final MemorySettingsProperties memorySettings;
  private final TaskContextService taskContext;
  private final TaskContextProperties taskContextSettings;
  private final ToolExecutionService tools;
  private final ObjectMapper objectMapper;
  private final AgentService agents;

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

  @Transactional
  public ChatResult sendMessage(
      Long conversationId,
      Long knowledgeBaseId,
      String userMessage,
      String provider,
      String modelName) {
    if (userMessage == null || userMessage.isBlank()) {
      throw new IllegalArgumentException("Message is required");
    }

    long convId;
    if (conversationId == null) {
      convId = conversations.create(defaultTitle(userMessage)).id();
    } else {
      convId = conversations.get(conversationId).id();
    }

    long userMessageId = messages.create(convId, "user", userMessage, null, null, null);

    String trimmed = userMessage.trim();
    if (trimmed.startsWith("/tool")) {
      String[] parts = trimmed.split("\\s+", 3);
      if (parts.length < 2) {
        throw new IllegalArgumentException("Usage: /tool <toolName> <json?>");
      }
      String toolName = parts[1];
      String jsonText = parts.length >= 3 ? parts[2].trim() : "{}";
      var input = tryParseJson(jsonText);
      var r = tools.execute("user", new ToolExecutionService.ExecuteRequest(toolName, input, null));
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

    if (trimmed.startsWith("/agent")) {
      String[] parts = trimmed.split("\\s+", 3);
      if (parts.length < 3) {
        throw new IllegalArgumentException("Usage: /agent <role|auto> <text>");
      }
      String role = parts[1];
      String text = parts[2];
      ModelResponse r = agents.run(role, text, provider, modelName);
      messages.create(convId, "assistant", r.content(), r.provider(), r.model(), null);
      return new ChatResult(convId, r, List.of());
    }

    List<Message> historyDesc = messages.listRecent(convId, settings.maxHistoryMessages());
    Collections.reverse(historyDesc);

    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(ChatMessage.system(settings.systemPrompt()));

    if (taskContextSettings.enabled()) {
      try {
        String ctx = taskContext.buildSystemContext(taskContextSettings.maxTasksInPrompt());
        if (ctx != null && !ctx.isBlank()) {
          prompt.add(ChatMessage.system(ctx));
        }
      } catch (RuntimeException ignored) {
        // Best-effort: task context should not break chatting.
      }
    }

    var approvedMemories = memoryService.listApproved(memorySettings.maxApprovedMemoriesInPrompt());
    if (!approvedMemories.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("已确认的长期记忆（可用于更好地回答用户）：\n");
      for (var m : approvedMemories) {
        if (m.title() != null && !m.title().isBlank()) {
          sb.append("- ").append(m.title()).append("：").append(m.content()).append("\n");
        } else {
          sb.append("- ").append(m.content()).append("\n");
        }
      }
      prompt.add(ChatMessage.system(sb.toString()));
    }

    List<Citation> citations = List.of();
    if (knowledgeBaseId != null) {
      var hits = retrieval.retrieve(knowledgeBaseId, userMessage, 5);
      if (!hits.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是知识库检索结果（请优先使用这些信息回答，并在回答中引用对应来源）：\n");
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
    for (Message m : historyDesc) {
      prompt.add(new ChatMessage(m.role(), m.content()));
    }

    ModelResponse response = modelService.chat(provider, modelName, prompt);
    messages.create(
        convId, "assistant", response.content(), response.provider(), response.model(), null);

    for (var c : memoryExtraction.extractCandidates(userMessage, response.content())) {
      memoryService.createCandidate(
          c.title(), c.content(), "chat", convId, userMessageId, c.reason());
    }

    return new ChatResult(convId, response, citations);
  }

  private static String defaultTitle(String userMessage) {
    String trimmed = userMessage.trim();
    if (trimmed.length() <= 24) {
      return trimmed;
    }
    return trimmed.substring(0, 24);
  }

  private JsonNode tryParseJson(String text) {
    try {
      return objectMapper.readTree(text);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON for /tool input");
    }
  }

  public record ChatResult(long conversationId, ModelResponse response, List<Citation> citations) {}

  public record Citation(
      String documentId,
      String documentTitle,
      String sourceUri,
      String chunkId,
      String excerpt,
      double score) {}
}
