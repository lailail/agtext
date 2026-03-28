package com.agtext.memory.domain;

import java.time.Instant;

/**
 * 记忆条目领域模型：
 * 采用 Java Record 实现，确保条目在内存中的不可变性（Immutable）。
 * 该模型承载了从对话提取到审核生效全生命周期的所有关键字段。
 */
public record MemoryItem(
        // 数据库主键 ID
        long id,

        // 记忆摘要或标题，方便人工审核和快速检索
        String title,

        // 核心记忆文本内容
        String content,

        // 状态机：例如 'candidate' (待审), 'approved' (已生效), 'disabled' (已禁用)
        String status,

        // 来源类型：如 'conversation' (对话提取), 'manual' (人工输入), 'system' (系统生成)
        String sourceType,

        // 溯源字段：记录该记忆是从哪个会话、哪条消息中提取出来的
        Long sourceConversationId,
        Long sourceMessageId,

        // 业务关联 ID（三级层级结构）：
        // 允许记忆挂载到具体的战略目标、执行计划或原子任务上
        Long relatedGoalId,
        Long relatedPlanId,
        Long relatedTaskId,

        // AI 提取逻辑说明：记录 AI 为什么要将这段信息转为长期记忆
        String candidateReason,

        // 审核批注：记录人工审核通过或禁用时的理由
        String reviewerNote,

        // 生命周期时间戳
        Instant reviewedAt,  // 审核时间
        Instant approvedAt,  // 生效时间
        Instant disabledAt,  // 禁用时间
        Instant createdAt,   // 创建时间
        Instant updatedAt    // 最后更新时间
) {}