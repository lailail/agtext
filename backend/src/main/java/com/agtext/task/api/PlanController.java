package com.agtext.task.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.task.domain.PlanItem;
import com.agtext.task.service.PlanService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
  private static final String PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";
  private final PlanService service;

  public PlanController(PlanService service) {
    this.service = service;
  }

  @PostMapping
  public PlanItemResponse create(@RequestBody CreatePlanRequest req) {
    Long goalId =
        req.goalId() == null || req.goalId().isBlank()
            ? null
            : IdCodec.decode(GOAL_PREFIX, req.goalId());
    return toItem(service.create(goalId, req.title(), req.description()));
  }

  @GetMapping
  public PageResponse<PlanItemResponse> list(
      @RequestParam(name = "goal_id", required = false) String goalId,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    Long rawGoal = goalId == null || goalId.isBlank() ? null : IdCodec.decode(GOAL_PREFIX, goalId);
    List<PlanItemResponse> items =
        service.list(rawGoal, status, page, pageSize).stream().map(this::toItem).toList();
    return new PageResponse<>(items, page, pageSize, service.count(rawGoal, status));
  }

  @GetMapping("/{id}")
  public PlanItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  @PostMapping("/{id}")
  public PlanItemResponse update(
      @PathVariable("id") String id, @RequestBody UpdatePlanRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    Long goalId =
        req.goalId() == null || req.goalId().isBlank()
            ? null
            : IdCodec.decode(GOAL_PREFIX, req.goalId());
    return toItem(service.update(raw, goalId, req.title(), req.description(), req.status()));
  }

  private PlanItemResponse toItem(PlanItem p) {
    return new PlanItemResponse(
        IdCodec.encode(PREFIX, p.id()),
        p.goalId() == null ? null : IdCodec.encode(GOAL_PREFIX, p.goalId()),
        p.title(),
        p.description(),
        p.status(),
        p.createdAt(),
        p.updatedAt());
  }

  public record CreatePlanRequest(String goalId, String title, String description) {}

  public record UpdatePlanRequest(String goalId, String title, String description, String status) {}

  public record PlanItemResponse(
      String id,
      String goalId,
      String title,
      String description,
      String status,
      Instant createdAt,
      Instant updatedAt) {}
}
