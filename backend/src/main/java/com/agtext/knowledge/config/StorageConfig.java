package com.agtext.knowledge.config;

import com.agtext.knowledge.service.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 存储模块配置类：负责初始化与文件存储相关的基础设施配置。
 */
@Configuration
// 激活 StorageProperties 类的配置属性绑定功能。
// 该注解确保以特定前缀（如 app.storage）开头的配置文件属性能够自动注入到 StorageProperties 实例中。
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
    // 此类主要作为配置元数据的入口点，通常不包含复杂的逻辑方法。
}