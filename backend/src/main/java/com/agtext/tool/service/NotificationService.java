package com.agtext.tool.service;

import com.agtext.tool.domain.NotificationItem;
import com.agtext.tool.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository repo;

  public NotificationService(NotificationRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public long create(
      String type, String title, String content, String refType, String refId, String status) {
    String normalizedStatus = status == null || status.isBlank() ? "unread" : status;
    return repo.create(type, title, content, refType, refId, normalizedStatus);
  }

  @Transactional(readOnly = true)
  public List<NotificationItem> list(String status, String type, int page, int pageSize) {
    return repo.list(status, type, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long count(String status, String type) {
    return repo.count(status, type);
  }

  @Transactional(readOnly = true)
  public long unreadCount() {
    return repo.countUnread();
  }

  @Transactional
  public void markRead(long id) {
    repo.updateStatus(id, "read");
  }

  @Transactional
  public void archive(long id) {
    repo.updateStatus(id, "archived");
  }
}
