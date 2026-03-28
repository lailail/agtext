package com.agtext.tool.domain;

import java.time.Instant;

/**
 * 通知项实体（Notification Item）
 * 承载系统发给用户的各类提醒、告警或业务通知
 */
public record NotificationItem(
        // 内部唯一自增 ID
        long id,

        // 通知类型：例如 "system"（系统公告）, "reminder"（任务提醒）, "task_update"（任务变更）
        String type,

        // 通知标题：简洁明了的概要说明
        String title,

        // 通知正文：详细的描述信息，支持长文本或富文本
        String content,

        // 关联资源类型：指示通知对应的业务模块（如 Task, Plan），用于前端逻辑跳转
        String refType,

        // 关联资源 ID：业务模块内具体的对象标识
        String refId,

        // 通知的当前状态：unread（未读）, read（已读）, archived（已归档）
        String status,

        // 通知生成的时间
        Instant createdAt,

        // 通知状态最后变更的时间（如标记已读的时间）
        Instant updatedAt) {}