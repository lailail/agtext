package com.agtext.chat.domain;

import java.time.Instant;

/**
 * 消息领域模型。
 * 承载对话流中的单次交互数据，是构建大模型上下文（Context Window）的最小单位。
 */
public record Message(
        /** 消息全局唯一 ID（内部长整型） */
        long id,

        /** 所属会话 ID，用于维护对话的逻辑关联 */
        long conversationId,

        /** * 成员角色。
         * 常见值：'system' (系统指令), 'user' (用户提问), 'assistant' (模型回复)。
         */
        String role,

        /** 消息文本主体，存储原始请求或生成的 Markdown 内容 */
        String content,

        /** 模型供应商标识，如 "openai", "deepseek" 等 */
        String provider,

        /** 具体模型名称，如 "gpt-4o", "claude-3-5-sonnet" */
        String modelName,

        /** * 本条消息消耗的 Token 计数。
         * 实事求是地讲：该字段对成本审计和长对话截断策略（Truncation）至关重要。
         */
        Integer tokens,

        /** 消息存入数据库的时刻 */
        Instant createdAt,

        /** 消息最后一次修正或更新的时刻 */
        Instant updatedAt
) {}