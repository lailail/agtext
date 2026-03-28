package com.agtext.task.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.task.domain.GoalItem;
import com.agtext.task.service.GoalService;
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
@RequestMapping("/api/goals")
public class GoalController {
  private static final String PREFIX = "goal_";
  private final GoalService service;

  public GoalController(GoalService service) {
    this.service = service;
  }

  @PostMapping
  public GoalItemResponse create(@RequestBody CreateGoalRequest req) {
    return toItem(service.create(req.title(), req.description()));
  }

  @GetMapping
  public PageResponse<GoalItemResponse> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    List<GoalItemResponse> items =
        service.list(status, page, pageSize).stream().map(this::toItem).toList();
    return new PageResponse<>(items, page, pageSize, service.count(status));
  }

  @GetMapping("/{id}")
  public GoalItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  @PostMapping("/{id}")
  public GoalItemResponse update(
      @PathVariable("id") String id, @RequestBody UpdateGoalRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.update(raw, req.title(), req.description(), req.status()));
  }

  private GoalItemResponse toItem(GoalItem g) {
    return new GoalItemResponse(
        IdCodec.encode(PREFIX, g.id()),
        g.title(),
        g.description(),
        g.status(),
        g.createdAt(),
        g.updatedAt());
  }

  public record CreateGoalRequest(String title, String description) {}

  public record UpdateGoalRequest(String title, String description, String status) {}

  public record GoalItemResponse(
      String id,
      String title,
      String description,
      String status,
      Instant createdAt,
      Instant updatedAt) {}
}
