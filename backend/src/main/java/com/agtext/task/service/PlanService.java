package com.agtext.task.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.task.domain.PlanItem;
import com.agtext.task.repository.PlanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 计划（Plan）业务逻辑服务
 * 职责：
 * 1. 维护计划的生命周期，确保计划与其所属目标（Goal）的逻辑一致性。
 * 2. 在创建或更新时执行跨模块（GoalService）的存在性校验。
 */
@Service
public class PlanService {
  private final PlanRepository repo;
  private final GoalService goals;

  public PlanService(PlanRepository repo, GoalService goals) {
    this.repo = repo;
    this.goals = goals;
  }

  /**
   * 创建新计划
   * @param goalId 所属目标 ID（可选）
   * @param title 计划标题（必填）
   * @param description 计划详情说明
   * 逻辑：
   * 1. 校验标题非空。
   * 2. 若关联了 goalId，调用 goals.get(goalId) 强制检查目标是否存在，不存在则抛出异常。
   */
  @Transactional
  public PlanItem create(Long goalId, String title, String description) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    // 跨模块校验：确保上级 Goal 记录真实有效
    if (goalId != null) {
      goals.get(goalId);
    }
    long id = repo.create(goalId, title.trim(), description);
    return get(id);
  }

  /**
   * 获取计划详情
   * 事务：只读。
   */
  @Transactional(readOnly = true)
  public PlanItem get(long id) {
    return repo.findById(id)
            .orElseThrow(() -> new NotFoundException("PLAN_NOT_FOUND", "Plan not found"));
  }

  /**
   * 分页查询计划列表
   * 可根据 goalId 过滤属于特定目标的计划。
   */
  @Transactional(readOnly = true)
  public List<PlanItem> list(Long goalId, String status, int page, int pageSize) {
    return repo.list(goalId, status, page, pageSize);
  }

  /**
   * 统计符合条件的计划总数
   */
  @Transactional(readOnly = true)
  public long count(Long goalId, String status) {
    return repo.count(goalId, status);
  }

  /**
   * 更新计划属性
   * 逻辑：
   * 1. 检查当前计划 ID 是否存在。
   * 2. 若变更或保留了 goalId，重新校验目标的存在性。
   * 3. 规范化状态（status），默认设为 "active"。
   */
  @Transactional
  public PlanItem update(long id, Long goalId, String title, String description, String status) {
    get(id); // 存在性预检
    if (goalId != null) {
      goals.get(goalId); // 关联性预检
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    String st = status == null || status.isBlank() ? "active" : status;
    repo.update(id, goalId, title.trim(), description, st);
    return get(id);
  }
}