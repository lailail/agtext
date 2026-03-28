package com.agtext.task.config;

import com.agtext.task.service.TaskContextProperties;
import com.agtext.task.service.TaskReminderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 任务模块核心配置类
 * * 职责：
 * 1. 作为 Spring 配置源，加载任务系统相关的 Bean 定义。
 * 2. 激活定时任务调度机制，用于处理提醒、逾期状态更新等异步逻辑。
 * 3. 绑定外部配置文件（如 application.yml）中的自定义属性到 Java 对象。
 */
@Configuration
/* * 开启 Spring 任务调度功能。
 * 允许在 Service 层或 Component 层使用 @Scheduled 注解执行定时任务。
 */
@EnableScheduling
/* * 启用配置属性绑定。
 * TaskContextProperties: 任务上下文相关配置（如默认限制、系统参数）。
 * TaskReminderProperties: 任务提醒触发逻辑配置（如提前提醒时间、频率控制）。
 */
@EnableConfigurationProperties({TaskContextProperties.class, TaskReminderProperties.class})
public class TaskConfig {
    // 此处目前为纯配置声明，后续如需自定义 ThreadPoolTaskScheduler，可在此定义 @Bean。
}