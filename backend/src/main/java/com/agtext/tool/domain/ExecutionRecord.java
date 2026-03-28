package com.agtext.tool.domain;

import java.time.Instant;

/**
 * 执行记录实体（Execution Record）
 * 用于审计、监控以及幂等性检查，记录系统内所有关键动作的执行轨迹
 */
public record ExecutionRecord(
        // 内部唯一自增 ID
        long id,

        // 执行主体：例如 "user", "agent", "system" 或具体的 UserId
        String actor,

        // 调用来源：例如 "web", "mobile", "api_gateway"
        String source,

        // 动作类型：如 "CREATE_TASK", "DELETE_GOAL"
        String actionType,

        // 关联资源类型：标识受影响的领域对象（如 Task）
        String refType,

        // 关联资源 ID：受影响对象的业务 ID
        String refId,

        // 幂等键：由调用方提供，防止相同请求被重复执行
        String idempotencyKey,

        // 输入摘要：执行前的关键参数简述（通常不存储大容量 Payload）
        String inputSummary,

        // 输出摘要：执行成功后的结果简述或关键返回信息
        String outputSummary,

        // 执行状态：success（成功）, failed（失败）, running（执行中）
        String status,

        // 错误代码：当 status 为 failed 时记录的具体错误标识
        String errorCode,

        // 执行耗时：单位为毫秒（ms），用于系统性能监控
        Long durationMs,

        // 记录创建时间
        Instant createdAt,

        // 记录最后更新时间
        Instant updatedAt) {}