package com.agtext.task.api;

import com.agtext.common.ids.IdCodec;
import com.agtext.task.domain.TaskItem;
import com.agtext.task.service.TaskService;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inbox")
public class InboxController {
  private static final String TASK_PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";

  private final TaskService tasks;

  public InboxController(TaskService tasks) {
    this.tasks = tasks;
  }

  @PostMapping
  public TaskItemResponse create(@RequestBody CreateInboxRequest req) {
    TaskItem t =
        tasks.create(null, null, true, req.title(), req.description(), "todo", 0, null, null);
    return toItem(t);
  }

  @PostMapping("/{task_id}/archive")
  public TaskItemResponse archive(
      @PathVariable("task_id") String taskId, @RequestBody ArchiveRequest req) {
    long raw = IdCodec.decode(TASK_PREFIX, taskId);
    Long planId =
        req.planId() == null || req.planId().isBlank()
            ? null
            : IdCodec.decode(PLAN_PREFIX, req.planId());
    Long goalId =
        req.goalId() == null || req.goalId().isBlank()
            ? null
            : IdCodec.decode(GOAL_PREFIX, req.goalId());
    return toItem(tasks.archiveInbox(raw, planId, goalId));
  }

  @PostMapping("/{task_id}/delete")
  public TaskItemResponse delete(@PathVariable("task_id") String taskId) {
    long raw = IdCodec.decode(TASK_PREFIX, taskId);
    return toItem(tasks.deleteInbox(raw));
  }

  private TaskItemResponse toItem(TaskItem t) {
    return new TaskItemResponse(
        IdCodec.encode(TASK_PREFIX, t.id()),
        t.planId() == null ? null : IdCodec.encode(PLAN_PREFIX, t.planId()),
        t.goalId() == null ? null : IdCodec.encode(GOAL_PREFIX, t.goalId()),
        t.inbox(),
        t.title(),
        t.description(),
        t.status(),
        t.priority(),
        t.dueAt(),
        t.remindAt(),
        t.snoozeUntil(),
        t.createdAt(),
        t.updatedAt());
  }

  public record CreateInboxRequest(String title, String description) {}

  public record ArchiveRequest(String planId, String goalId) {}

  public record TaskItemResponse(
      String id,
      String planId,
      String goalId,
      boolean inbox,
      String title,
      String description,
      String status,
      int priority,
      Instant dueAt,
      Instant remindAt,
      Instant snoozeUntil,
      Instant createdAt,
      Instant updatedAt) {}
}
