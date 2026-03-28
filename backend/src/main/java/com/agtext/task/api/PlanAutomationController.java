package com.agtext.task.api;

import com.agtext.common.ids.IdCodec;
import com.agtext.task.service.PlanAutomationService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  @PostMapping("/decompose")
  public DecomposeResponse decompose(@RequestBody DecomposeRequest req) {
    var r = service.decompose(req.objective(), req.provider(), req.model(), req.maxSteps());
    return new DecomposeResponse(
        r.title(), r.steps().stream().map(s -> new StepItem(s.title(), s.description())).toList());
  }

  @PostMapping("/from-objective")
  public PlanWithTasksResponse fromObjective(@RequestBody FromObjectiveRequest req) {
    Long goalId =
        req.goalId() == null || req.goalId().isBlank()
            ? null
            : IdCodec.decode(GOAL_PREFIX, req.goalId());
    var r =
        service.createFromObjective(
            goalId, req.objective(), req.provider(), req.model(), req.maxSteps());
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
