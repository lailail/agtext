package com.agtext.task.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.task.domain.TaskItem;
import com.agtext.task.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务业务逻辑层
 * 封装核心业务规则，管理数据库事务
 */
@Service
public class TaskService {
  private final TaskRepository repo;
  private final PlanService plans;
  private final GoalService goals;

  public TaskService(TaskRepository repo, PlanService plans, GoalService goals) {
    this.repo = repo;
    this.plans = plans;
    this.goals = goals;
  }

  /**
   * 创建任务
   * 包含：必填项校验、外键关联存在性检查、默认值填充
   */
  @Transactional
  public TaskItem create(
          Long planId,
          Long goalId,
          boolean inbox,
          String title,
          String description,
          String status,
          Integer priority,
          Instant dueAt,
          Instant remindAt) {
    // 强制校验：标题不能为空
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    // 引用完整性检查：确保关联的 Plan 和 Goal 存在
    if (planId != null) {
      plans.get(planId);
    }
    if (goalId != null) {
      goals.get(goalId);
    }
    // 默认值处理
    String st = status == null || status.isBlank() ? "todo" : status;
    int pr = priority == null ? 0 : priority;

    long id =
            repo.create(
                    planId, goalId, inbox, title.trim(), description, st, pr, dueAt, remindAt, null);
    return get(id);
  }

  /**
   * 获取任务详情
   * 若任务不存在则抛出 404 业务异常
   */
  @Transactional(readOnly = true)
  public TaskItem get(long id) {
    return repo.findById(id)
            .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
  }

  /**
   * 列表查询（只读事务）
   */
  @Transactional(readOnly = true)
  public List<TaskItem> list(
          Long planId,
          Long goalId,
          Boolean inbox,
          String status,
          Instant dueBefore,
          int page,
          int pageSize) {
    return repo.list(planId, goalId, inbox, status, dueBefore, page, pageSize);
  }

  /**
   * 统计总数（只读事务）
   */
  @Transactional(readOnly = true)
  public long count(Long planId, Long goalId, Boolean inbox, String status, Instant dueBefore) {
    return repo.count(planId, goalId, inbox, status, dueBefore);
  }

  /**
   * 更新任务
   * 包含：状态校验（先 get(id) 确认存在）、关联检查、标题重校验
   */
  @Transactional
  public TaskItem update(
          long id,
          Long planId,
          Long goalId,
          boolean inbox,
          String title,
          String description,
          String status,
          Integer priority,
          Instant dueAt,
          Instant remindAt,
          Instant snoozeUntil) {
    get(id); // 检查任务是否存在
    if (planId != null) {
      plans.get(planId);
    }
    if (goalId != null) {
      goals.get(goalId);
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    String st = status == null || status.isBlank() ? "todo" : status;
    int pr = priority == null ? 0 : priority;

    repo.update(
            id, planId, goalId, inbox, title.trim(), description, st, pr, dueAt, remindAt, snoozeUntil);
    return get(id);
  }

  /**
   * 完成任务
   * 更新状态为 'done' 并清除所有提醒
   */
  @Transactional
  public TaskItem complete(long id) {
    get(id);
    repo.updateStatusAndClearReminder(id, "done");
    return get(id);
  }

  /**
   * 延后（推迟）提醒
   * 将 remind_at 和 snooze_until 同步更新
   */
  @Transactional
  public TaskItem snooze(long id, Instant snoozeUntil) {
    get(id);
    if (snoozeUntil == null) {
      throw new IllegalArgumentException("snoozeUntil is required");
    }
    repo.updateReminder(id, snoozeUntil, snoozeUntil);
    return get(id);
  }

  /**
   * 取消提醒
   * 将 remind_at 和 snooze_until 置为 null
   */
  @Transactional
  public TaskItem dismissReminder(long id) {
    get(id);
    repo.updateReminder(id, null, null);
    return get(id);
  }

  /**
   * 查询今日到期任务列表
   */
  @Transactional(readOnly = true)
  public List<TaskItem> listDueToday(Instant from, Instant to, int limit) {
    return repo.listDueBetween(from, to, limit);
  }

  /**
   * 查询已过期任务列表
   */
  @Transactional(readOnly = true)
  public List<TaskItem> listOverdue(Instant now, int limit) {
    return repo.listOverdue(now, limit);
  }

  /**
   * 查询即将触发的提醒
   */
  @Transactional(readOnly = true)
  public List<TaskItem> listUpcomingReminders(Instant from, Instant to, int limit) {
    return repo.listRemindBetween(from, to, limit);
  }

  /**
   * 归档收件箱项目
   * 将其从收件箱移出，并正式关联到具体的计划或目标
   */
  @Transactional
  public TaskItem archiveInbox(long id, Long planId, Long goalId) {
    get(id);
    if (planId != null) {
      plans.get(planId);
    }
    if (goalId != null) {
      goals.get(goalId);
    }
    repo.archiveInboxItem(id, planId, goalId);
    return get(id);
  }

  /**
   * 删除收件箱项目（逻辑删除）
   * 设置状态为 'cancelled' 并清除提醒
   */
  @Transactional
  public TaskItem deleteInbox(long id) {
    get(id);
    repo.deleteInboxItem(id);
    return get(id);
  }

  /**
   * 查询当前需要触发提醒的任务列表
   */
  @Transactional(readOnly = true)
  public List<TaskItem> listReminderDue(Instant now, int limit) {
    return repo.listRemindDue(now, limit);
  }

  /**
   * 尝试记录提醒触发事件
   * 利用 DataIntegrityViolationException (如唯一键冲突) 防止重复记录
   * 返回 true 表示记录成功（首次触发），false 表示已存在或失败
   */
  @Transactional
  public boolean tryRecordReminderEvent(long taskId, Instant remindAt, Instant firedAt) {
    try {
      int rows = repo.insertReminderEvent(taskId, remindAt, "remind_at", "triggered", firedAt);
      return rows > 0;
    } catch (DataIntegrityViolationException e) {
      // 捕获数据库层面的约束异常，视为事件已处理
      return false;
    }
  }
}