package com.agtext.chat.api;

import com.agtext.chat.domain.Message;
import com.agtext.chat.service.ConversationService;
import com.agtext.chat.service.MessageService;
import com.agtext.common.ids.IdCodec;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息明细控制器。
 * 采用嵌套路由设计 `/api/conversations/{conversationId}/messages`，
 * 明确了消息（Message）对会话（Conversation）的从属关系。
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {
  // 区分会话与消息的 ID 前缀，防止在解码阶段发生资源类型混淆
  private static final String CONV_PREFIX = "cnv_";
  private static final String MSG_PREFIX = "msg_";

  private final ConversationService conversations;
  private final MessageService messages;

  public MessageController(ConversationService conversations, MessageService messages) {
    this.conversations = conversations;
    this.messages = messages;
  }

  /**
   * 获取指定会话下的所有历史消息。
   * @param conversationId 外部传入的加密会话 ID
   * @return 经过 DTO 转换后的有序消息列表（通常按创建时间升序）
   */
  @GetMapping
  public List<MessageItem> list(@PathVariable("conversationId") String conversationId) {
    // 1. 解码并还原物理主键
    long convId = IdCodec.decode(CONV_PREFIX, conversationId);

    // 2. 前置存在性检查：确保会话存在（若不存在 get 方法通常抛出 404 异常），
    // 同时也作为权限隔离的切入点（检查当前用户是否有权访问该 convId）
    conversations.get(convId);

    // 3. 业务调用与流式转换
    return messages.listByConversationId(convId).stream().map(MessageController::toItem).toList();
  }

  /**
   * 静态转换工具：将消息领域模型映射为前端友好的 Item 对象。
   * 补充了 Token 消耗量、模型名称等可观测性字段。
   */
  private static MessageItem toItem(Message m) {
    return new MessageItem(
            IdCodec.encode(MSG_PREFIX, m.id()),            // 消息本身的混淆 ID
            IdCodec.encode(CONV_PREFIX, m.conversationId()), // 所属会话的混淆 ID
            m.role(),       // 角色：user, assistant, system 等
            m.content(),    // 消息文本内容
            m.provider(),   // 推理供应商（如 OpenAI）
            m.modelName(),  // 具体模型型号（如 gpt-4o）
            m.tokens(),     // 本条消息消耗的 Token 数（用于统计）
            m.createdAt(),
            m.updatedAt());
  }

  /**
   * 消息列表项载体
   */
  public record MessageItem(
          String id,
          String conversationId,
          String role,
          String content,
          String provider,
          String modelName,
          Integer tokens,
          Instant createdAt,
          Instant updatedAt) {}
}