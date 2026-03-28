package com.agtext.memory.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.memory.domain.MemoryItem;
import com.agtext.memory.repository.MemoryItemRepository;
import com.agtext.task.service.GoalService;
import com.agtext.task.service.PlanService;
import com.agtext.task.service.TaskService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 记忆业务服务：
 * 实现 AI 记忆从创建、审核、修改到业务关联的完整生命周期管理。
 * 通过 @Transactional 确保数据操作的原子性。
 */
@Service
public class MemoryService {
  private final MemoryItemRepository repo;
  private final GoalService goals;
  private final PlanService plans;
  private final TaskService tasks;

  public MemoryService(
          MemoryItemRepository repo, GoalService goals, PlanService plans, TaskService tasks) {
    this.repo = repo;
    this.goals = goals;
    this.plans = plans;
    this.tasks = tasks;
  }

  /**
   * 创建候选记忆条目
   * 逻辑：清洗数据 -> 存入数据库 -> 返回完整的领域模型
   */
  @Transactional
  public MemoryItem createCandidate(
          String title,
          String content,
          String sourceType,
          Long sourceConversationId,
          Long sourceMessageId,
          String candidateReason) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content is required");
    }
    long id =
            repo.createCandidate(
                    title == null || title.isBlank() ? null : title.trim(),
                    content.trim(),
                    sourceType,
                    sourceConversationId,
                    sourceMessageId,
                    candidateReason);
    return get(id);
  }

  /**
   * 获取单条记忆，若不存在则抛出统一的业务异常
   */
  @Transactional(readOnly = true)
  public MemoryItem get(long id) {
    return repo.findById(id)
            .orElseThrow(() -> new NotFoundException("MEMORY_NOT_FOUND", "Memory item not found"));
  }

  /**
   * 分页列表查询
   */
  @Transactional(readOnly = true)
  public List<MemoryItem> list(String status, int page, int pageSize) {
    return repo.list(status, page, pageSize);
  }

  /**
   * 统计总数（用于前端分页控件）
   */
  @Transactional(readOnly = true)
  public long count(String status) {
    return repo.count(status);
  }

  /**
   * 审核通过：将候选状态转为正式状态
   */
  @Transactional
  public MemoryItem approve(long id, String reviewerNote) {
    get(id); // 检查是否存在
    repo.approve(id, reviewerNote);
    return get(id);
  }

  /**
   * 禁用条目：废弃一段记忆
   */
  @Transactional
  public MemoryItem disable(long id, String reviewerNote) {
    get(id); // 检查是否存在
    repo.disable(id, reviewerNote);
    return get(id);
  }

  /**
   * 更新记忆内容（支持人工修正 AI 提取时的偏差）
   */
  @Transactional
  public MemoryItem update(long id, String title, String content) {
    get(id);
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content is required");
    }
    repo.updateContent(id, title, content.trim());
    return get(id);
  }

  /**
   * 业务关联：建立记忆与“任务/计划/目标”的多级关联。
   * 严肃性检查：在建立关联前，会调用外部 Service 确认关联的目标、计划或任务真实有效。
   */
  @Transactional
  public MemoryItem linkToTask(
          long id, Long relatedGoalId, Long relatedPlanId, Long relatedTaskId) {
    get(id); // 校验记忆本身是否存在

    // 跨模块完整性校验
    if (relatedGoalId != null) {
      goals.get(relatedGoalId);
    }
    if (relatedPlanId != null) {
      plans.get(relatedPlanId);
    }
    if (relatedTaskId != null) {
      tasks.get(relatedTaskId);
    }

    repo.updateLinks(id, relatedGoalId, relatedPlanId, relatedTaskId);
    return get(id);
  }

  /**
   * 获取已通过审核的所有记忆（通常用于构建检索索引）
   */
  @Transactional(readOnly = true)
  public List<MemoryItem> listApproved(int limit) {
    return repo.listApproved(limit);
  }
}