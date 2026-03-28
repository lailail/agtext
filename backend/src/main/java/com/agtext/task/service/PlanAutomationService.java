package com.agtext.task.service;

import com.agtext.task.domain.PlanItem;
import com.agtext.task.domain.TaskItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 计划自动化服务
 * 职责：
 * 1. 调用 AI 分解服务（PlanDecompositionService）将复杂的宏观目标拆解为可执行的步骤。
 * 2. 协调 PlanService 和 TaskService，将拆解结果持久化到数据库。
 */
@Service
public class PlanAutomationService {
  private final PlanDecompositionService decomposer;
  private final PlanService plans;
  private final TaskService tasks;

  public PlanAutomationService(
          PlanDecompositionService decomposer, PlanService plans, TaskService tasks) {
    this.decomposer = decomposer;
    this.plans = plans;
    this.tasks = tasks;
  }

  /**
   * 执行纯逻辑层面的任务拆解（不涉及数据库写入）
   * @param objective 目标描述文本
   * @param provider AI 提供商名称
   * @param model 使用的 AI 模型标识
   * @param maxSteps 最大拆解步骤限制（默认为 8）
   */
  public PlanDecompositionService.DecompositionResult decompose(
          String objective, String provider, String model, Integer maxSteps) {
    int limit = maxSteps == null ? 8 : maxSteps;
    return decomposer.decompose(objective, provider, model, limit);
  }

  /**
   * 核心业务方法：根据给定目标自动创建计划及其关联任务
   * 事务控制：由于涉及 Plan 和多个 Task 的插入，必须保证操作的原子性。
   * * 流程：
   * 1. 调用 AI 接口获取拆解后的标题和步骤。
   * 2. 校验并生成计划标题。
   * 3. 持久化 Plan 记录。
   * 4. 循环遍历 AI 返回的步骤，逐一持久化为 Task 记录。
   */
  @Transactional
  public PlanWithTasks createFromObjective(
          Long goalId, String objective, String provider, String model, Integer maxSteps) {

    // 1. 获取拆解结果
    PlanDecompositionService.DecompositionResult r =
            decompose(objective, provider, model, maxSteps);

    // 2. 标题容错处理
    String title = r.title();
    if (title == null || title.isBlank()) {
      title = defaultTitle(objective);
    }

    // 3. 创建计划主体
    PlanItem plan = plans.create(goalId, title, objective);

    // 4. 批量创建关联任务
    List<TaskItem> created = new ArrayList<>();
    for (var s : r.steps()) {
      TaskItem t =
              tasks.create(
                      plan.id(),
                      goalId,
                      false,          // 明确指定不进入收件箱（Inbox）
                      s.title(),
                      s.description(),
                      "todo",         // 初始状态统一为待办
                      0,              // 默认优先级
                      null,           // 初始不设置截止时间
                      null            // 初始不设置提醒
              );
      created.add(t);
    }

    return new PlanWithTasks(plan, created);
  }

  /**
   * 当 AI 未能返回有效标题时，截取目标描述作为默认标题
   */
  private static String defaultTitle(String objective) {
    if (objective == null) {
      return "Plan";
    }
    String t = objective.trim();
    if (t.length() <= 32) {
      return t;
    }
    return t.substring(0, 32);
  }

  /**
   * 响应载体：组合返回创建成功的计划及任务列表
   */
  public record PlanWithTasks(PlanItem plan, List<TaskItem> tasks) {}
}