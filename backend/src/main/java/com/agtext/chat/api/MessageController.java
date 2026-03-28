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

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {
  private static final String CONV_PREFIX = "cnv_";
  private static final String MSG_PREFIX = "msg_";

  private final ConversationService conversations;
  private final MessageService messages;

  public MessageController(ConversationService conversations, MessageService messages) {
    this.conversations = conversations;
    this.messages = messages;
  }

  @GetMapping
  public List<MessageItem> list(@PathVariable("conversationId") String conversationId) {
    long convId = IdCodec.decode(CONV_PREFIX, conversationId);
    conversations.get(convId);
    return messages.listByConversationId(convId).stream().map(MessageController::toItem).toList();
  }

  private static MessageItem toItem(Message m) {
    return new MessageItem(
        IdCodec.encode(MSG_PREFIX, m.id()),
        IdCodec.encode(CONV_PREFIX, m.conversationId()),
        m.role(),
        m.content(),
        m.provider(),
        m.modelName(),
        m.tokens(),
        m.createdAt(),
        m.updatedAt());
  }

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
