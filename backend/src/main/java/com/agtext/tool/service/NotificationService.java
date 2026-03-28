package com.agtext.tool.service;

import com.agtext.tool.domain.NotificationItem;
import com.agtext.tool.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息通知服务类
 * 处理系统通知的增、删、改、查业务逻辑
 */
@Service
public class NotificationService {

  private final NotificationRepository repo;

  /**
   * 使用构造器注入，确保 Repository 在 Service 实例化时即不可变且非空
   */
  public NotificationService(NotificationRepository repo) {
    this.repo = repo;
  }

  /**
   * 创建新通知
   * @param status 初始状态，若为空则强制默认为 "unread"
   * @return 生成的通知主键 ID
   */
  @Transactional
  public long create(
          String type, String title, String content, String refType, String refId, String status) {
    // 状态归一化处理：防止空值或空字符串进入数据库，确保状态机初始值正确
    String normalizedStatus = (status == null || status.isBlank()) ? "unread" : status;
    return repo.create(type, title, content, refType, refId, normalizedStatus);
  }

  /**
   * 分页查询通知列表
   * readOnly = true 告知数据库驱动此为只读操作，有助于优化性能（如在 MySQL 环境下减少锁竞争）
   */
  @Transactional(readOnly = true)
  public List<NotificationItem> list(String status, String type, int page, int pageSize) {
    return repo.list(status, type, page, pageSize);
  }

  /**
   * 根据条件统计通知总数
   */
  @Transactional(readOnly = true)
  public long count(String status, String type) {
    return repo.count(status, type);
  }

  /**
   * 获取当前所有未读消息的数量
   */
  @Transactional(readOnly = true)
  public long unreadCount() {
    return repo.countUnread();
  }

  /**
   * 将指定通知标记为已读
   * @param id 通知记录的主键
   */
  @Transactional
  public void markRead(long id) {
    // 硬编码状态字符串 "read"，确保业务语义的一致性
    repo.updateStatus(id, "read");
  }

  /**
   * 归档通知记录
   * 归档后通常在常规列表查询中不可见，取决于 Repository 层的过滤逻辑
   */
  @Transactional
  public void archive(long id) {
    repo.updateStatus(id, "archived");
  }
}