package com.agtext.task.domain;

import java.time.Instant;

/**
 * 目标（Goal）领域模型
 * * 职责：
 * 1. 作为任务系统中的顶层容器，用于聚合多个相关的任务（Task）。
 * 2. 存储目标的元数据，包括标题、描述及当前生命周期状态。
 * * 技术说明：
 * - 使用 Java Record 确保数据的不可变性（Immutable），适用于并发读取和无副作用的逻辑处理。
 * - 时间字段使用 Instant 格式，以确保时区无关的 UTC 时间戳存储。
 */
public record GoalItem(
        /** 数据库唯一标识 ID */
        long id,

        /** 目标的简短名称 */
        String title,

        /** 目标的详细背景或执行说明 */
        String description,

        /** 目标当前状态（例如：active, archived, completed） */
        String status,

        /** 记录创建的精确时间点 */
        Instant createdAt,

        /** 记录最后一次变更的时间点 */
        Instant updatedAt) {

    /**
     * 注意：由于是 record，编译器会自动生成：
     * 1. 全参数构造函数
     * 2. 所有字段的 getter 方法（如 id(), title()）
     * 3. equals(), hashCode() 和 toString() 方法
     */
}