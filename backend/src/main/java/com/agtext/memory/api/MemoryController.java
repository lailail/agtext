package com.agtext.memory.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.memory.domain.MemoryItem;
import com.agtext.memory.service.MemoryService;
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
@RequestMapping("/api/memory/items")
public class MemoryController {
  private static final String PREFIX = "mem_";
  private static final String TASK_PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";
  private final MemoryService service;

  public MemoryController(MemoryService service) {
    this.service = service;
  }

  @PostMapping
  public MemoryItemResponse create(@RequestBody CreateMemoryRequest req) {
    MemoryItem item =
        service.createCandidate(
            req.title(),
            req.content(),
            req.sourceType(),
            req.sourceConversationId(),
            req.sourceMessageId(),
            req.candidateReason());
    return toItem(item);
  }

  @GetMapping
  public PageResponse<MemoryItemResponse> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    List<MemoryItemResponse> items =
        service.list(status, page, pageSize).stream().map(this::toItem).toList();
    long total = service.count(status);
    return new PageResponse<>(items, page, pageSize, total);
  }

  @GetMapping("/{id}")
  public MemoryItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  @PostMapping("/{id}/approve")
  public MemoryItemResponse approve(@PathVariable("id") String id, @RequestBody ReviewRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.approve(raw, req.reviewerNote()));
  }

  @PostMapping("/{id}/disable")
  public MemoryItemResponse disable(@PathVariable("id") String id, @RequestBody ReviewRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.disable(raw, req.reviewerNote()));
  }

  @PostMapping("/{id}")
  public MemoryItemResponse update(
      @PathVariable("id") String id, @RequestBody UpdateMemoryRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.update(raw, req.title(), req.content()));
  }

  @PostMapping("/{id}/link")
  public MemoryItemResponse link(@PathVariable("id") String id, @RequestBody LinkRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    Long rawGoal = decodeNullable(GOAL_PREFIX, req.goalId());
    Long rawPlan = decodeNullable(PLAN_PREFIX, req.planId());
    Long rawTask = decodeNullable(TASK_PREFIX, req.taskId());
    return toItem(service.linkToTask(raw, rawGoal, rawPlan, rawTask));
  }

  private MemoryItemResponse toItem(MemoryItem m) {
    return new MemoryItemResponse(
        IdCodec.encode(PREFIX, m.id()),
        m.title(),
        m.content(),
        m.status(),
        m.sourceType(),
        m.sourceConversationId(),
        m.sourceMessageId(),
        m.relatedGoalId() == null ? null : IdCodec.encode(GOAL_PREFIX, m.relatedGoalId()),
        m.relatedPlanId() == null ? null : IdCodec.encode(PLAN_PREFIX, m.relatedPlanId()),
        m.relatedTaskId() == null ? null : IdCodec.encode(TASK_PREFIX, m.relatedTaskId()),
        m.candidateReason(),
        m.reviewerNote(),
        m.reviewedAt(),
        m.approvedAt(),
        m.disabledAt(),
        m.createdAt(),
        m.updatedAt());
  }

  private static Long decodeNullable(String prefix, String id) {
    return id == null || id.isBlank() ? null : IdCodec.decode(prefix, id);
  }

  public record CreateMemoryRequest(
      String title,
      String content,
      String sourceType,
      Long sourceConversationId,
      Long sourceMessageId,
      String candidateReason) {}

  public record UpdateMemoryRequest(String title, String content) {}

  public record ReviewRequest(String reviewerNote) {}

  public record LinkRequest(String goalId, String planId, String taskId) {}

  public record MemoryItemResponse(
      String id,
      String title,
      String content,
      String status,
      String sourceType,
      Long sourceConversationId,
      Long sourceMessageId,
      String relatedGoalId,
      String relatedPlanId,
      String relatedTaskId,
      String candidateReason,
      String reviewerNote,
      Instant reviewedAt,
      Instant approvedAt,
      Instant disabledAt,
      Instant createdAt,
      Instant updatedAt) {}
}
