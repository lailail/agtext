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

/**
 * 计划管理控制器：
 * 负责处理执行方案（Plan）的生命周期。
 * 计划通常是将宏大的“目标”拆解为可操作步骤的集合。
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {
  private static final String PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";
  private final PlanService service;

  public PlanController(PlanService service) {
    this.service = service;
  }

  /**
   * 创建计划：
   * 必须处理外部传入的编码 ID (goal_...) 并将其解码为数据库长整型 ID。
   */
  @PostMapping
  public PlanItemResponse create(@RequestBody CreatePlanRequest req) {
    Long goalId =
            req.goalId() == null || req.goalId().isBlank()
                    ? null
                    : IdCodec.decode(GOAL_PREFIX, req.goalId());
    return toItem(service.create(goalId, req.title(), req.description()));
  }

  /**
   * 分页查询计划列表：
   * 支持按所属目标（goal_id）或计划状态（status）进行组合过滤。
   */
  @GetMapping
  public PageResponse<PlanItemResponse> list(
          @RequestParam(name = "goal_id", required = false) String goalId,
          @RequestParam(name = "status", required = false) String status,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {

    // 预处理：解码查询参数中的目标 ID
    Long rawGoal = goalId == null || goalId.isBlank() ? null : IdCodec.decode(GOAL_PREFIX, goalId);

    List<PlanItemResponse> items =
            service.list(rawGoal, status, page, pageSize).stream().map(this::toItem).toList();

    return new PageResponse<>(items, page, pageSize, service.count(rawGoal, status));
  }

  /**
   * 获取计划详情：
   * 根据 plan_ 格式的 ID 进行查询。
   */
  @GetMapping("/{id}")
  public PlanItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  /**
   * 更新计划信息：
   * 支持重新关联目标，或者修改计划的标题、描述及状态。
   */
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

  /**
   * 响应数据转换：
   * 确保返回给前端的所有关联 ID 都经过了编码处理，保持外部接口的统一性。
   */
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

  // --- DTO 定义 ---

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