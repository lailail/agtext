package com.agtext.task.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.task.domain.TaskItem;
import com.agtext.task.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    if (planId != null) {
      plans.get(planId);
    }
    if (goalId != null) {
      goals.get(goalId);
    }
    String st = status == null || status.isBlank() ? "todo" : status;
    int pr = priority == null ? 0 : priority;
    long id =
        repo.create(
            planId, goalId, inbox, title.trim(), description, st, pr, dueAt, remindAt, null);
    return get(id);
  }

  @Transactional(readOnly = true)
  public TaskItem get(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
  }

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

  @Transactional(readOnly = true)
  public long count(Long planId, Long goalId, Boolean inbox, String status, Instant dueBefore) {
    return repo.count(planId, goalId, inbox, status, dueBefore);
  }

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
    get(id);
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

  @Transactional
  public TaskItem complete(long id) {
    get(id);
    repo.updateStatusAndClearReminder(id, "done");
    return get(id);
  }

  @Transactional
  public TaskItem snooze(long id, Instant snoozeUntil) {
    get(id);
    if (snoozeUntil == null) {
      throw new IllegalArgumentException("snoozeUntil is required");
    }
    repo.updateReminder(id, snoozeUntil, snoozeUntil);
    return get(id);
  }

  @Transactional
  public TaskItem dismissReminder(long id) {
    get(id);
    repo.updateReminder(id, null, null);
    return get(id);
  }

  @Transactional(readOnly = true)
  public List<TaskItem> listDueToday(Instant from, Instant to, int limit) {
    return repo.listDueBetween(from, to, limit);
  }

  @Transactional(readOnly = true)
  public List<TaskItem> listOverdue(Instant now, int limit) {
    return repo.listOverdue(now, limit);
  }

  @Transactional(readOnly = true)
  public List<TaskItem> listUpcomingReminders(Instant from, Instant to, int limit) {
    return repo.listRemindBetween(from, to, limit);
  }

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

  @Transactional
  public TaskItem deleteInbox(long id) {
    get(id);
    repo.deleteInboxItem(id);
    return get(id);
  }

  @Transactional(readOnly = true)
  public List<TaskItem> listReminderDue(Instant now, int limit) {
    return repo.listRemindDue(now, limit);
  }

  @Transactional
  public boolean tryRecordReminderEvent(long taskId, Instant remindAt, Instant firedAt) {
    try {
      int rows = repo.insertReminderEvent(taskId, remindAt, "remind_at", "triggered", firedAt);
      return rows > 0;
    } catch (DataIntegrityViolationException e) {
      return false;
    }
  }
}
