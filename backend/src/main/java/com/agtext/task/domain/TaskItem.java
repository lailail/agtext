package com.agtext.task.domain;

import java.time.Instant;

/**
 * 任务（Task）领域模型
 * * 职责：
 * 1. 作为系统中最小的可执行单元，记录具体的待办事项。
 * 2. 维护任务的状态生命周期（Status）、优先级（Priority）及时间维度（截止、提醒、推迟）。
 * 3. 关联上级容器（Goal 或 Plan），或归属于收件箱（Inbox）。
 * * 技术特性：
 * - 使用 Java Record 确保任务实体的不可变性，防止在 Service 层之外被意外篡改。
 * - 所有的 ID 关联均采用原始长整型（long/Long），以便于数据库索引和分级存储。
 */
public record TaskItem(
        /** 任务唯一标识 ID */
        long id,

        /** 关联的计划 ID，若任务不属于任何计划则为 null */
        Long planId,

        /** 关联的目标 ID，若任务不属于任何目标则为 null */
        Long goalId,

        /** 是否属于收件箱。true 表示任务尚未归类到具体计划或目标中 */
        boolean inbox,

        /** 任务标题，核心描述文字 */
        String title,

        /** 任务详细说明，可存储长文本备注 */
        String description,

        /** 任务状态（如：todo, in_progress, completed, cancelled） */
        String status,

        /** 优先级数值，通常数值越大代表优先级越高 */
        int priority,

        /** 任务截止日期时间戳 */
        Instant dueAt,

        /** 预设的提醒触发时间点 */
        Instant remindAt,

        /** 推迟截止时间点。当用户执行 snooze 操作时更新此字段 */
        Instant snoozeUntil,

        /** 任务创建的 UTC 时间戳 */
        Instant createdAt,

        /** 任务最近一次修改的 UTC 时间戳 */
        Instant updatedAt
) {
    /**
     * 业务逻辑约束：
     * 1. 当 inbox 为 true 时，通常 planId 和 goalId 应当为空。
     * 2. 状态流转应受业务状态机控制，例如已完成（completed）的任务不应再触发提醒（remindAt）。
     */
}