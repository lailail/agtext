package com.agtext.chat.api;

import com.agtext.chat.service.ChatOrchestrator;
import com.agtext.common.ids.IdCodec;
import com.agtext.tool.service.ExecutionRecordService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
  private static final String CONV_PREFIX = "cnv_";
  private static final String KB_PREFIX = "kb_";
  private final ChatOrchestrator orchestrator;
  private final ExecutionRecordService executions;

  public ChatController(ChatOrchestrator orchestrator, ExecutionRecordService executions) {
    this.orchestrator = orchestrator;
    this.executions = executions;
  }

  @PostMapping
  public ChatResponse chat(@RequestBody ChatRequest req) {
    long start = System.currentTimeMillis();
    Long conversationId = null;
    if (req.conversationId() != null && !req.conversationId().isBlank()) {
      conversationId = IdCodec.decode(CONV_PREFIX, req.conversationId());
    }
    Long kbId = null;
    if (req.knowledgeBaseId() != null && !req.knowledgeBaseId().isBlank()) {
      kbId = IdCodec.decode(KB_PREFIX, req.knowledgeBaseId());
    }
    try {
      var result =
          orchestrator.sendMessage(
              conversationId, kbId, req.message(), req.provider(), req.model());
      executions.record(
          "user",
          "ui",
          "chat.send",
          "conversation",
          IdCodec.encode(CONV_PREFIX, result.conversationId()),
          null,
          "message=" + (req.message() == null ? "" : req.message().trim()),
          "provider=" + result.response().provider() + ", model=" + result.response().model(),
          "succeeded",
          null,
          System.currentTimeMillis() - start);
      return new ChatResponse(
          IdCodec.encode(CONV_PREFIX, result.conversationId()),
          result.response().provider(),
          result.response().model(),
          result.response().content(),
          result.citations(),
          Instant.now());
    } catch (RuntimeException e) {
      executions.record(
          "user",
          "ui",
          "chat.send",
          "conversation",
          req.conversationId(),
          null,
          "message=" + (req.message() == null ? "" : req.message().trim()),
          null,
          "failed",
          e.getClass().getSimpleName(),
          System.currentTimeMillis() - start);
      throw e;
    }
  }

  public record ChatRequest(
      String conversationId,
      String knowledgeBaseId,
      String message,
      String provider,
      String model) {}

  public record ChatResponse(
      String conversationId,
      String provider,
      String model,
      String assistantMessage,
      List<ChatOrchestrator.Citation> citations,
      Instant createdAt) {}
}
