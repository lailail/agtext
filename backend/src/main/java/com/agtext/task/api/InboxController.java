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

/**
 * 收集箱控制器：
 * 处理快速创建的临时任务（Inbox）。
 * 这些任务通常只有标题和描述，没有关联具体的计划或目标，等待后续的“处理与组织（Process & Organize）”。
 */
@RestController
@RequestMapping("/api/inbox")
public class InboxController {
  // 定义 ID 前缀，用于混淆不同层级的对象 ID
  private static final String TASK_PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";

  private final TaskService tasks;

  public InboxController(TaskService tasks) {
    this.tasks = tasks;
  }

  /**
   * 快速创建收集箱条目：
   * 默认关联 planId=null, goalId=null，且 inbox 标记为 true。
   */
  @PostMapping
  public TaskItemResponse create(@RequestBody CreateInboxRequest req) {
    // 调用 TaskService 创建一个状态为 'todo'、优先级为 0 的收集箱任务
    TaskItem t =
            tasks.create(null, null, true, req.title(), req.description(), "todo", 0, null, null);
    return toItem(t);
  }

  /**
   * 归档收集箱条目：
   * 将一个零散的任务分配到具体的计划（Plan）或目标（Goal）下。
   * 归档操作通常会将任务的 inbox 状态改为 false。
   */
  @PostMapping("/{task_id}/archive")
  public TaskItemResponse archive(
          @PathVariable("task_id") String taskId, @RequestBody ArchiveRequest req) {
    // 1. 解码任务 ID
    long raw = IdCodec.decode(TASK_PREFIX, taskId);

    // 2. 解码可选的关联 ID（Plan 和 Goal）
    Long planId =
            req.planId() == null || req.planId().isBlank()
                    ? null
                    : IdCodec.decode(PLAN_PREFIX, req.planId());
    Long goalId =
            req.goalId() == null || req.goalId().isBlank()
                    ? null
                    : IdCodec.decode(GOAL_PREFIX, req.goalId());

    // 3. 执行归档业务逻辑
    return toItem(tasks.archiveInbox(raw, planId, goalId));
  }

  /**
   * 删除收集箱条目：
   * 物理删除或逻辑删除收集箱中的任务。
   */
  @PostMapping("/{task_id}/delete")
  public TaskItemResponse delete(@PathVariable("task_id") String taskId) {
    long raw = IdCodec.decode(TASK_PREFIX, taskId);
    return toItem(tasks.deleteInbox(raw));
  }

  /**
   * 内部转换方法：
   * 将领域对象 TaskItem 转换为带前缀编码 ID 的响应 DTO。
   */
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

  // --- DTO 定义 ---

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