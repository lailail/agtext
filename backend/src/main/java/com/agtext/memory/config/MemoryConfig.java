package com.agtext.memory.config;

import com.agtext.memory.service.MemorySettingsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 记忆模块自动配置类：
 * 负责在 Spring 上下文中启用和配置记忆（Memory）相关的组件。
 */
@Configuration
// 激活 @ConfigurationProperties 绑定功能。
// 只有在这里声明了 MemorySettingsProperties.class，
// Spring 才会将其扫描并作为一个 Bean 注入到容器中，供 MemoryService 等组件使用。
@EnableConfigurationProperties(MemorySettingsProperties.class)
public class MemoryConfig {
    // 作为一个配置类，此处通常还可以定义其他 Bean，
    // 例如特定的 MemoryStrategy（记忆策略）或清理任务的调度器。
}