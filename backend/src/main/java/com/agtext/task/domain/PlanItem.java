package com.agtext.task.domain;

import java.time.Instant;

/**
 * 计划（Plan）领域模型
 * * 职责：
 * 1. 作为任务聚合的中层容器，通常隶属于某个特定的“目标（Goal）”。
 * 2. 用于组织具有相同阶段性目的的一组任务，提供逻辑隔离。
 * * 设计要点：
 * - 继承了 Java Record 的不可变特性，确保在并发环境下的线程安全。
 * - goalId 采用包装类型 Long，以支持该计划不属于任何具体目标的业务场景（即 null 值）。
 */
public record PlanItem(
        /** 计划的唯一标识 ID */
        long id,

        /** 关联的目标 ID，允许为空（null）表示独立计划 */
        Long goalId,

        /** 计划的简短标题 */
        String title,

        /** 计划的具体背景或执行路径说明 */
        String description,

        /** 计划状态（例如：draft, active, archived, completed） */
        String status,

        /** 记录创建的 UTC 时间戳 */
        Instant createdAt,

        /** 记录最近一次修改的 UTC 时间戳 */
        Instant updatedAt
) {
    /**
     * 业务规则：
     * 1. 如果 goalId 为空，该计划被视为“自由计划”。
     * 2. 状态流转应由 PlanService 结合业务逻辑进行校验。
     */
}