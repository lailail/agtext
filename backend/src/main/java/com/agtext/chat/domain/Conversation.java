package com.agtext.chat.domain;

import java.time.Instant;

/**
 * 会话领域模型（Domain Entity）。
 * 使用 Java Record 定义，确保了数据的不可变性（Immutability），适用于在 Service 与 Repository 间传递。
 * * @param id        全局唯一标识符（数据库主键，长整型）
 * @param title     会话标题（通常由 AI 自动生成或用户手动指定）
 * @param status    会话生命周期状态（如：'active' 活跃, 'archived' 已归档, 'deleted' 已软删除）
 * @param createdAt 记录创建时刻，用于历史排序
 * @param updatedAt 记录最后更新时刻（如最后一条消息生成的时间）
 */
public record Conversation(
        long id,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    /* * 实事求是地评估：
     * 虽然 Record 默认提供了全参数构造器，但在实际业务中，
     * 'status' 字段通常应配合特定的常量池或枚举（如 ConversationStatus）使用，
     * 以防止硬编码字符串导致的逻辑错误。
     */
}