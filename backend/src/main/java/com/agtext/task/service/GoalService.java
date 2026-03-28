package com.agtext.task.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.task.domain.GoalItem;
import com.agtext.task.repository.GoalRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 目标（Goal）业务逻辑服务
 * 职责：
 * 1. 封装目标创建与更新的业务规则校验。
 * 2. 管理数据库事务，确保数据一致性。
 * 3. 协调 Repository 层进行持久化操作。
 */
@Service
public class GoalService {
  private final GoalRepository repo;

  public GoalService(GoalRepository repo) {
    this.repo = repo;
  }

  /**
   * 创建新目标
   * 校验：标题不能为空或空白字符。
   * 事务：读写事务。
   */
  @Transactional
  public GoalItem create(String title, String description) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    long id = repo.create(title.trim(), description);
    return get(id); // 创建后重新获取完整对象（包含数据库生成的 ID 和时间戳）
  }

  /**
   * 获取目标详情
   * 校验：若 ID 不存在，抛出 NotFoundException。
   * 事务：只读事务。
   */
  @Transactional(readOnly = true)
  public GoalItem get(long id) {
    return repo.findById(id)
            .orElseThrow(() -> new NotFoundException("GOAL_NOT_FOUND", "Goal not found"));
  }

  /**
   * 分页查询目标列表
   */
  @Transactional(readOnly = true)
  public List<GoalItem> list(String status, int page, int pageSize) {
    return repo.list(status, page, pageSize);
  }

  /**
   * 获取符合条件的目标总数
   */
  @Transactional(readOnly = true)
  public long count(String status) {
    return repo.count(status);
  }

  /**
   * 更新目标属性
   * 逻辑：
   * 1. 首先通过 get(id) 确认目标存在。
   * 2. 校验标题合法性。
   * 3. 状态默认为 "active"。
   */
  @Transactional
  public GoalItem update(long id, String title, String description, String status) {
    get(id); // 存在性预检
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    String st = status == null || status.isBlank() ? "active" : status;
    repo.update(id, title.trim(), description, st);
    return get(id);
  }
}