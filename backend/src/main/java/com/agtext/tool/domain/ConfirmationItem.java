package com.agtext.tool.domain;

import java.time.Instant;

/**
 * 二次确认单实体（Domain Model）
 * 使用 Java Record 保证数据的不可变性，用于拦截敏感操作并等待人工或系统授权
 */
public record ConfirmationItem(
        // 内部唯一标识 ID
        long id,

        // 确认状态：例如 pending（待处理）, approved（已批准）, denied（已拒绝）
        String status,

        // 操作类型：描述该确认单关联的具体业务动作（如：DELETE_TASK, UPDATE_PLAN）
        String actionType,

        // 关联资源类型：如 Task, Goal, Plan 等
        String refType,

        // 关联资源 ID：具体被操作对象的唯一标识
        String refId,

        // 摘要信息：展示给用户看的人类可读说明，解释为什么要进行此操作
        String summary,

        // 原始请求载荷：以 JSON 或序列化字符串存储，待批准后用于还原执行原始请求
        String payload,

        // 创建时间
        Instant createdAt,

        // 最后更新时间
        Instant updatedAt) {}