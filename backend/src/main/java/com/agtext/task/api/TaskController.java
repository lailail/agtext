package com.agtext.task.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.task.domain.TaskItem;
import com.agtext.task.service.TaskService;
import com.agtext.tool.service.ExecutionRecordService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理控制器
 * 负责处理任务的增删改查、状态流转（完成、推迟）以及与前端交互的 ID 编解码。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  // 业务对象 ID 前缀，用于 IdCodec 混淆/编码
  private static final String PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";

  private final TaskService service;
  private final ExecutionRecordService executions;

  public TaskController(TaskService service, ExecutionRecordService executions) {
    this.service = service;
    this.executions = executions;
  }

  /**
   * 创建新任务
   * 逻辑：将外部字符串 ID 解码为数据库长整型 ID，并转换时间格式后调用服务层。
   */
  @PostMapping
  public TaskItemResponse create(@RequestBody CreateTaskRequest req) {
    return toItem(
            service.create(
                    decodeNullable(PLAN_PREFIX, req.planId()),
                    decodeNullable(GOAL_PREFIX, req.goalId()),
                    req.inbox() != null && req.inbox(),
                    req.title(),
                    req.description(),
                    req.status(),
                    req.priority(),
                    parseInstant(req.dueAt()),
                    parseInstant(req.remindAt())));
  }

  /**
   * 分页查询任务列表
   * 支持按计划、目标、收件箱状态、任务状态及截止时间过滤。
   */
  @GetMapping
  public PageResponse<TaskItemResponse> list(
          @RequestParam(name = "plan_id", required = false) String planId,
          @RequestParam(name = "goal_id", required = false) String goalId,
          @RequestParam(name = "inbox", required = false) Boolean inbox,
          @RequestParam(name = "status", required = false) String status,
          @RequestParam(name = "due_before", required = false) String dueBefore,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {

    Instant due = parseInstant(dueBefore);
    Long rawPlan = decodeNullable(PLAN_PREFIX, planId);
    Long rawGoal = decodeNullable(GOAL_PREFIX, goalId);

    List<TaskItemResponse> items =
            service.list(rawPlan, rawGoal, inbox, status, due, page, pageSize).stream()
                    .map(this::toItem)
                    .toList();

    long total = service.count(rawPlan, rawGoal, inbox, status, due);
    return new PageResponse<>(items, page, pageSize, total);
  }

  /**
   * 获取单个任务详情
   */
  @GetMapping("/{id}")
  public TaskItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  /**
   * 更新任务属性
   */
  @PostMapping("/{id}")
  public TaskItemResponse update(
          @PathVariable("id") String id, @RequestBody UpdateTaskRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(
            service.update(
                    raw,
                    decodeNullable(PLAN_PREFIX, req.planId()),
                    decodeNullable(GOAL_PREFIX, req.goalId()),
                    req.inbox() != null && req.inbox(),
                    req.title(),
                    req.description(),
                    req.status(),
                    req.priority(),
                    parseInstant(req.dueAt()),
                    parseInstant(req.remindAt()),
                    parseInstant(req.snoozeUntil())));
  }

  /**
   * 完成任务
   * 包含操作执行记录（ExecutionRecord），监控操作耗时与结果。
   */
  @PostMapping("/{id}/complete")
  public TaskItemResponse complete(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    long start = System.currentTimeMillis();
    try {
      TaskItem t = service.complete(raw);
      recordExecution(raw, "task.complete", null, "succeeded", null, start);
      return toItem(t);
    } catch (RuntimeException e) {
      recordExecution(raw, "task.complete", null, "failed", e.getClass().getSimpleName(), start);
      throw e;
    }
  }

  /**
   * 推迟任务
   */
  @PostMapping("/{id}/snooze")
  public TaskItemResponse snooze(@PathVariable("id") String id, @RequestBody SnoozeRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    Instant until = parseInstant(req.snoozeUntil());
    long start = System.currentTimeMillis();
    try {
      TaskItem t = service.snooze(raw, until);
      recordExecution(raw, "task.snooze", "snoozeUntil=" + req.snoozeUntil(), "succeeded", null, start);
      return toItem(t);
    } catch (RuntimeException e) {
      recordExecution(raw, "task.snooze", "snoozeUntil=" + req.snoozeUntil(), "failed", e.getClass().getSimpleName(), start);
      throw e;
    }
  }

  /**
   * 关闭/忽略任务提醒
   */
  @PostMapping("/{id}/dismiss-reminder")
  public TaskItemResponse dismissReminder(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    long start = System.currentTimeMillis();
    try {
      TaskItem t = service.dismissReminder(raw);
      recordExecution(raw, "task.dismissReminder", null, "succeeded", null, start);
      return toItem(t);
    } catch (RuntimeException e) {
      recordExecution(raw, "task.dismissReminder", null, "failed", e.getClass().getSimpleName(), start);
      throw e;
    }
  }

  /**
   * 获取“今日”概览
   * 包含：今日到期、已逾期、以及 24 小时内的即将提醒任务。
   */
  @GetMapping("/today")
  public TodayResponse today() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate d = LocalDate.now(zone);
    Instant start = d.atStartOfDay(zone).toInstant();
    Instant end = d.plusDays(1).atStartOfDay(zone).toInstant();
    Instant now = Instant.now();

    List<TaskItemResponse> todayDue =
            service.listDueToday(start, end, 200).stream().map(this::toItem).toList();
    List<TaskItemResponse> overdue =
            service.listOverdue(now, 200).stream().map(this::toItem).toList();
    List<TaskItemResponse> upcoming =
            service.listUpcomingReminders(now, now.plusSeconds(24 * 3600L), 200).stream()
                    .map(this::toItem)
                    .toList();

    return new TodayResponse(todayDue, overdue, upcoming);
  }

  // --- 辅助方法 ---

  /**
   * 将领域模型 TaskItem 转换为前端响应对象，并对相关 ID 进行编码。
   */
  private TaskItemResponse toItem(TaskItem t) {
    return new TaskItemResponse(
            IdCodec.encode(PREFIX, t.id()),
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

  /**
   * 统一封装执行记录写入逻辑。
   */
  private void recordExecution(long rawId, String action, String params, String status, String error, long startTime) {
    executions.record(
            "user", "ui", action, "task",
            IdCodec.encode(PREFIX, rawId),
            null, params, null,
            status, error, System.currentTimeMillis() - startTime);
  }

  private static Long decodeNullable(String prefix, String id) {
    return id == null || id.isBlank() ? null : IdCodec.decode(prefix, id);
  }

  /**
   * 解析 ISO-8601 格式的时间字符串为 Instant。
   */
  private static Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return OffsetDateTime.parse(value).toInstant();
  }

  // --- DTO 定义 ---

  public record CreateTaskRequest(
          String planId, String goalId, Boolean inbox, String title,
          String description, String status, Integer priority, String dueAt, String remindAt) {}

  public record UpdateTaskRequest(
          String planId, String goalId, Boolean inbox, String title,
          String description, String status, Integer priority, String dueAt, String remindAt, String snoozeUntil) {}

  public record SnoozeRequest(String snoozeUntil) {}

  public record TaskItemResponse(
          String id, String planId, String goalId, boolean inbox, String title,
          String description, String status, int priority, Instant dueAt,
          Instant remindAt, Instant snoozeUntil, Instant createdAt, Instant updatedAt) {}

  public record TodayResponse(
          List<TaskItemResponse> todayDue,
          List<TaskItemResponse> overdue,
          List<TaskItemResponse> upcomingReminders) {}
}