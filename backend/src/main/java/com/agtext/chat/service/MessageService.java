package com.agtext.chat.service;

import com.agtext.chat.domain.Message;
import com.agtext.chat.repository.MessageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {
  private final MessageRepository repo;

  public MessageService(MessageRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public long create(
      long conversationId,
      String role,
      String content,
      String provider,
      String modelName,
      Integer tokens) {
    return repo.create(conversationId, role, content, provider, modelName, tokens);
  }

  @Transactional(readOnly = true)
  public List<Message> listByConversationId(long conversationId) {
    return repo.listByConversationId(conversationId);
  }

  @Transactional(readOnly = true)
  public List<Message> listRecent(long conversationId, int limit) {
    return repo.listRecentByConversationId(conversationId, limit);
  }
}
