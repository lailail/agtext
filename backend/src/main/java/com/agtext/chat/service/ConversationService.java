package com.agtext.chat.service;

import com.agtext.chat.domain.Conversation;
import com.agtext.chat.repository.ConversationRepository;
import com.agtext.common.api.NotFoundException;
import com.agtext.common.ids.IdCodec;
import com.agtext.tool.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {
  private static final String CONV_PREFIX = "cnv_";
  private final ConversationRepository repo;
  private final NotificationService notifications;

  public ConversationService(ConversationRepository repo, NotificationService notifications) {
    this.repo = repo;
    this.notifications = notifications;
  }

  @Transactional
  public Conversation create(String title) {
    long id = repo.create(title);
    Conversation c =
        repo.findById(id).orElseThrow(() -> new IllegalStateException("Conversation not found"));
    notifications.create(
        "system",
        "新会话已创建",
        c.title(),
        "conversation",
        IdCodec.encode(CONV_PREFIX, c.id()),
        "unread");
    return c;
  }

  @Transactional(readOnly = true)
  public Conversation get(long id) {
    return repo.findById(id)
        .orElseThrow(
            () -> new NotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
  }

  @Transactional(readOnly = true)
  public List<Conversation> list(int page, int pageSize) {
    return repo.list(page, pageSize);
  }

  @Transactional(readOnly = true)
  public long countAll() {
    return repo.countAll();
  }
}
