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

/**
 * 聊天对话核心控制器。
 * 负责外部加密 ID 与内部长整型 ID 的转换，并集成了操作审计与执行记录功能。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
  // 资源前缀定义，用于 IdCodec 进行类型校验与防篡改
  private static final String CONV_PREFIX = "cnv_";
  private static final String KB_PREFIX = "kb_";

  private final ChatOrchestrator orchestrator; // 聊天编排器，负责召回、推理及上下文管理
  private final ExecutionRecordService executions; // 审计日志服务，记录所有 AI 交互细节

  public ChatController(ChatOrchestrator orchestrator, ExecutionRecordService executions) {
    this.orchestrator = orchestrator;
    this.executions = executions;
  }

  /**
   * 发送聊天消息接口。
   * 该方法包含了完整的生命周期监控：ID 解码 -> 业务调用 -> 成功/失败记录 -> 响应封装。
   */
  @PostMapping
  public ChatResponse chat(@RequestBody ChatRequest req) {
    long start = System.currentTimeMillis();

    // 1. 安全 ID 解码：将前端传递的混淆字符串（如 cnv_x7y2）还原为数据库物理 ID
    Long conversationId = null;
    if (req.conversationId() != null && !req.conversationId().isBlank()) {
      conversationId = IdCodec.decode(CONV_PREFIX, req.conversationId());
    }

    Long kbId = null;
    if (req.knowledgeBaseId() != null && !req.knowledgeBaseId().isBlank()) {
      kbId = IdCodec.decode(KB_PREFIX, req.knowledgeBaseId());
    }

    try {
      // 2. 核心业务编排：调用编排器处理对话逻辑（包括 RAG 检索和模型响应）
      var result =
              orchestrator.sendMessage(
                      conversationId, kbId, req.message(), req.provider(), req.model());

      // 3. 成功审计记录：记录耗时、模型信息及会话状态，用于后续性能分析和计费
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

      // 4. 返回 DTO 封装：包含加密后的会话 ID 和引用的知识库条目（Citations）
      return new ChatResponse(
              IdCodec.encode(CONV_PREFIX, result.conversationId()),
              result.response().provider(),
              result.response().model(),
              result.response().content(),
              result.citations(),
              Instant.now());

    } catch (RuntimeException e) {
      // 5. 异常审计记录：捕获推理或业务异常，确保存持久化失败原因
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
      throw e; // 继续向上抛出，由全局异常处理器封装为标准的错误响应
    }
  }

  /**
   * 聊天请求载体。
   * @param conversationId   可选，若为空则后端会自动创建新会话
   * @param knowledgeBaseId  可选，指定关联的知识库进行 RAG
   */
  public record ChatRequest(
          String conversationId,
          String knowledgeBaseId,
          String message,
          String provider,
          String model) {}

  /**
   * 聊天响应载体。
   * @param assistantMessage AI 生成的最终回复文本
   * @param citations        引用的参考来源列表，供前端渲染溯源信息
   */
  public record ChatResponse(
          String conversationId,
          String provider,
          String model,
          String assistantMessage,
          List<ChatOrchestrator.Citation> citations,
          Instant createdAt) {}
}