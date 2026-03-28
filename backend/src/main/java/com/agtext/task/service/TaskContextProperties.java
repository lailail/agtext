package com.agtext.task.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务上下文配置属性
 * 职责：
 * 1. 映射 application.yml 中以 'app.task.context' 为前缀的配置项。
 * 2. 控制是否向 AI 模型提供任务上下文，以及提供的数据规模。
 * * 技术说明：
 * - 使用 record 确保配置对象的不可变性。
 * - 配合 @EnableConfigurationProperties(TaskContextProperties.class) 使用。
 */
@ConfigurationProperties(prefix = "app.task.context")
public record TaskContextProperties(
        /** * 是否启用上下文注入。
         * 若为 false，AI 将无法感知用户的当前任务列表。
         */
        boolean enabled,

        /** * 允许注入到 Prompt 中的最大任务数量。
         * 实事求是：此参数用于防止 Prompt 过长导致 Token 浪费或模型处理性能下降。
         */
        int maxTasksInPrompt
) {}