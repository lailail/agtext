package com.agtext.task.api;

import com.agtext.common.ids.IdCodec;
import com.agtext.task.service.PlanAutomationService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计划自动化控制器：
 * 实现 AI 辅助的任务分解。它不直接操作 CRUD，而是调用 AI 能力来“策划”行动方案。
 * 该控制器是 AI Agent 从“思考”转为“行动”的关键桥梁。
 */
@RestController
@RequestMapping("/api/plans")
public class PlanAutomationController {
  private static final String GOAL_PREFIX = "goal_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String TASK_PREFIX = "task_";

  private final PlanAutomationService service;

  public PlanAutomationController(PlanAutomationService service) {
    this.service = service;
  }

  /**
   * 纯分解预览：
   * 仅调用 LLM 将一个目标分解为多个步骤，但不进行数据库持久化。
   * 用于前端展示 AI 的建议方案，供用户预览和修改。
   */
  @PostMapping("/decompose")
  public DecomposeResponse decompose(@RequestBody DecomposeRequest req) {
    var r = service.decompose(req.objective(), req.provider(), req.model(), req.maxSteps());
    return new DecomposeResponse(
            r.title(),
            r.steps().stream().map(s -> new StepItem(s.title(), s.description())).toList()
    );
  }

  /**
   * 自动生成计划并落库：
   * 一键完成：分解意图 -> 创建 Plan 记录 -> 创建一系列关联 Task 记录。
   * 支持指定所属的长期目标 (Goal)。
   */
  @PostMapping("/from-objective")
  public PlanWithTasksResponse fromObjective(@RequestBody FromObjectiveRequest req) {
    // 1. 解码 Goal ID（如果存在）
    Long goalId =
            req.goalId() == null || req.goalId().isBlank()
                    ? null
                    : IdCodec.decode(GOAL_PREFIX, req.goalId());

    // 2. 调用自动化服务执行复杂的分解与保存逻辑
    var r =
            service.createFromObjective(
                    goalId, req.objective(), req.provider(), req.model(), req.maxSteps());

    // 3. 将生成的领域对象（Plan + Tasks）统一编码为响应 DTO
    return new PlanWithTasksResponse(
            new PlanItemResponse(
                    IdCodec.encode(PLAN_PREFIX, r.plan().id()),
                    r.plan().goalId() == null ? null : IdCodec.encode(GOAL_PREFIX, r.plan().goalId()),
                    r.plan().title(),
                    r.plan().description(),
                    r.plan().status(),
                    r.plan().createdAt(),
                    r.plan().updatedAt()),
            r.tasks().stream()
                    .map(
                            t ->
                                    new TaskItemResponse(
                                            IdCodec.encode(TASK_PREFIX, t.id()),
                                            t.planId() == null ? null : IdCodec.encode(PLAN_PREFIX, t.planId()),
                                            t.goalId() == null ? null : IdCodec.encode(GOAL_PREFIX, t.goalId()),
                                            t.inbox(),
                                            t.title(),
                                            t.description(),
                                            t.status(),
                                            t.priority(),
                                            t.dueAt(),
                                            t.createdAt(),
                                            t.updatedAt()))
                    .toList());
  }

  // --- DTO 定义（内部 Record） ---

  public record DecomposeRequest(
          String objective, String provider, String model, Integer maxSteps) {}

  public record StepItem(String title, String description) {}

  public record DecomposeResponse(String title, List<StepItem> steps) {}

  public record FromObjectiveRequest(
          String goalId, String objective, String provider, String model, Integer maxSteps) {}

  public record PlanWithTasksResponse(PlanItemResponse plan, List<TaskItemResponse> tasks) {}

  public record PlanItemResponse(
          String id,
          String goalId,
          String title,
          String description,
          String status,
          Instant createdAt,
          Instant updatedAt) {}

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
          Instant createdAt,
          Instant updatedAt) {}
}