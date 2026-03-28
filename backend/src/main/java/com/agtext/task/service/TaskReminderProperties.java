package com.agtext.task.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务提醒配置属性
 * 职责：
 * 1. 映射 application.yml 中以 'app.task.reminder' 为前缀的配置。
 * 2. 核心参数用于配置后台定时扫描任务（Scheduled Task）的执行策略。
 */
@ConfigurationProperties(prefix = "app.task.reminder")
public record TaskReminderProperties(
        /** * 是否启用后台提醒扫描功能。
         * 实事求是：若为 false，即便任务设置了 remindAt，系统也不会主动触发提醒逻辑。
         */
        boolean enabled,

        /** * 单次扫描处理的最大任务数量。
         * 作用：防止单次处理过多任务导致数据库 I/O 阻塞或内存激增（防止长事务）。
         */
        int maxBatchSize,

        /** * 两次扫描之间的固定延迟时间（毫秒）。
         * 作用：控制后台线程的运行频率，平衡“实时性”与“系统资源开销”。
         */
        long fixedDelayMs
) {}