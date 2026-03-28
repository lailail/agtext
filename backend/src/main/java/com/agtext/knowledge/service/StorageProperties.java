package com.agtext.knowledge.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性类：
 * 用于映射配置文件中以 'app.storage' 为前缀的配置项。
 * * 使用 Record 结合 @ConfigurationProperties 是 Spring Boot 3.0+ 的推荐做法，
 * 能够提供不可变（Immutable）且简洁的配置载体。
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        /**
         * 存储根路径：
         * 对应配置文件中的 app.storage.root。
         * 所有的知识库文件、PDF、Markdown 原始文件都将存放在此目录下。
         */
        String root
) {}