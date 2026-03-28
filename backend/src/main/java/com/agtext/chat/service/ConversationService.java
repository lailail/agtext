package com.agtext.chat.service;

import com.agtext.chat.domain.Conversation;
import com.agtext.chat.repository.ConversationRepository;
import com.agtext.common.api.NotFoundException;
import com.agtext.common.ids.IdCodec;
import com.agtext.tool.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话管理服务：负责会话的生命周期管理，包括创建、查询及分页列表
 */
@Service
public class ConversationService {
  private static final String CONV_PREFIX = "cnv_"; // 会话 ID 编码前缀
  private final ConversationRepository repo;
  private final NotificationService notifications;

  public ConversationService(ConversationRepository repo, NotificationService notifications) {
    this.repo = repo;
    this.notifications = notifications;
  }

  /**
   * 创建新会话：
   * 1. 在数据库中持久化会话记录。
   * 2. 自动触发系统通知。
   * @param title 会话标题
   * @return 创建成功的会话实体
   */
  @Transactional
  public Conversation create(String title) {
    long id = repo.create(title);
    Conversation c =
            repo.findById(id).orElseThrow(() -> new IllegalStateException("Conversation not found"));

    // 发送系统通知：将内部 Long ID 编码为外部可访问的字符串格式 (如 cnv_xxx)
    notifications.create(
            "system",
            "新会话已创建",
            c.title(),
            "conversation",
            IdCodec.encode(CONV_PREFIX, c.id()),
            "unread");
    return c;
  }

  /**
   * 根据 ID 获取指定会话。
   * @throws NotFoundException 如果会话不存在则抛出业务异常
   */
  @Transactional(readOnly = true)
  public Conversation get(long id) {
    return repo.findById(id)
            .orElseThrow(
                    () -> new NotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
  }

  /**
   * 分页获取会话列表。
   * @param page 当前页码
   * @param pageSize 每页条数
   */
  @Transactional(readOnly = true)
  public List<Conversation> list(int page, int pageSize) {
    return repo.list(page, pageSize);
  }

  /**
   * 获取系统中总会话数量。
   */
  @Transactional(readOnly = true)
  public long countAll() {
    return repo.countAll();
  }
}