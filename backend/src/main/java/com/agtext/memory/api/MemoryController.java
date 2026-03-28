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

/**
 * 记忆管理控制器：
 * 处理 AI 记忆条目的生命周期管理，包括外部录入、人工审核及关联业务对象。
 */
@RestController
@RequestMapping("/api/memory/items")
public class MemoryController {
  // 定义不同业务对象的 ID 前缀，用于 HashID 编解码，增强 API 安全性并区分对象类型
  private static final String PREFIX = "mem_";
  private static final String TASK_PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";

  private final MemoryService service;

  public MemoryController(MemoryService service) {
    this.service = service;
  }

  /**
   * 创建记忆候选条目
   * 通常由 AI 代理在对话中提取并提交，初始状态一般为 "candidate"（待审核）
   */
  @PostMapping
  public MemoryItemResponse create(@RequestBody CreateMemoryRequest req) {
    MemoryItem item =
            service.createCandidate(
                    req.title(),
                    req.content(),
                    req.sourceType(),
                    req.sourceConversationId(),
                    req.sourceMessageId(),
                    req.candidateReason()); // 记录 AI 为什么要提取这段记忆
    return toItem(item);
  }

  /**
   * 分页查询记忆列表
   * @param status 状态过滤（如 candidate, approved, disabled）
   */
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

  /**
   * 获取特定记忆详情
   * @param id 外部 HashID（如 mem_abc123）
   */
  @GetMapping("/{id}")
  public MemoryItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id); // 解码为数据库长整型 ID
    return toItem(service.get(raw));
  }

  /**
   * 审核通过：使该记忆正式生效并可被 AI 检索使用
   */
  @PostMapping("/{id}/approve")
  public MemoryItemResponse approve(@PathVariable("id") String id, @RequestBody ReviewRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.approve(raw, req.reviewerNote()));
  }

  /**
   * 审核禁用：标记该记忆为无效或不再使用
   */
  @PostMapping("/{id}/disable")
  public MemoryItemResponse disable(@PathVariable("id") String id, @RequestBody ReviewRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.disable(raw, req.reviewerNote()));
  }

  /**
   * 更新记忆内容
   */
  @PostMapping("/{id}")
  public MemoryItemResponse update(
          @PathVariable("id") String id, @RequestBody UpdateMemoryRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.update(raw, req.title(), req.content()));
  }

  /**
   * 记忆关联：将记忆条目挂载到特定的目标、计划或任务上
   * 这有助于 AI 在处理特定任务时召回相关的历史背景
   */
  @PostMapping("/{id}/link")
  public MemoryItemResponse link(@PathVariable("id") String id, @RequestBody LinkRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    // 处理可选的关联 ID
    Long rawGoal = decodeNullable(GOAL_PREFIX, req.goalId());
    Long rawPlan = decodeNullable(PLAN_PREFIX, req.planId());
    Long rawTask = decodeNullable(TASK_PREFIX, req.taskId());
    return toItem(service.linkToTask(raw, rawGoal, rawPlan, rawTask));
  }

  /**
   * 将领域模型（MemoryItem）转换为 DTO（MemoryItemResponse）
   * 包含 ID 的加密编码过程
   */
  private MemoryItemResponse toItem(MemoryItem m) {
    return new MemoryItemResponse(
            IdCodec.encode(PREFIX, m.id()),
            m.title(),
            m.content(),
            m.status(),
            m.sourceType(),
            m.sourceConversationId(),
            m.sourceMessageId(),
            // 对关联对象的 ID 进行各自前缀的加密
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

  // --- Data Transfer Objects (DTOs) ---

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