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

/**
 * 目标管理控制器：
 * 处理 AI Agent 或用户设定的长期目标（Goal）的 CRUD 操作。
 * 目标是任务系统中最高层级的容器，通常包含多个计划（Plan）。
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {
  // 定义 ID 前缀，用于混淆和标识目标对象，如 "goal_abc123"
  private static final String PREFIX = "goal_";
  private final GoalService service;

  public GoalController(GoalService service) {
    this.service = service;
  }

  /**
   * 创建目标：
   * 接收标题和描述，返回创建成功的完整目标对象。
   */
  @PostMapping
  public GoalItemResponse create(@RequestBody CreateGoalRequest req) {
    return toItem(service.create(req.title(), req.description()));
  }

  /**
   * 分页查询目标列表：
   * 支持按状态过滤，并返回标准的分页包装对象。
   */
  @GetMapping
  public PageResponse<GoalItemResponse> list(
          @RequestParam(name = "status", required = false) String status,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {

    List<GoalItemResponse> items =
            service.list(status, page, pageSize).stream().map(this::toItem).toList();

    // 返回包含总条数的分页响应
    return new PageResponse<>(items, page, pageSize, service.count(status));
  }

  /**
   * 获取目标详情：
   * 使用 IdCodec 将外部加密字符串 ID（如 "goal_XyZ"）还原为内部数据库长整型 ID。
   */
  @GetMapping("/{id}")
  public GoalItemResponse get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  /**
   * 更新目标信息：
   * 允许修改标题、描述以及目标状态（如 'active', 'completed', 'dropped'）。
   */
  @PostMapping("/{id}")
  public GoalItemResponse update(
          @PathVariable("id") String id, @RequestBody UpdateGoalRequest req) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.update(raw, req.title(), req.description(), req.status()));
  }

  /**
   * 内部模型转换方法：
   * 将数据库实体（GoalItem）转换为对前端友好的响应 DTO（GoalItemResponse）。
   * 此处执行了 ID 的编码操作。
   */
  private GoalItemResponse toItem(GoalItem g) {
    return new GoalItemResponse(
            IdCodec.encode(PREFIX, g.id()), // 将长整型 123 转换为字符串 "goal_..."
            g.title(),
            g.description(),
            g.status(),
            g.createdAt(),
            g.updatedAt());
  }

  // --- DTO 定义（Data Transfer Objects） ---

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