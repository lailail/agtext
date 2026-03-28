package com.agtext.chat.config;

import com.agtext.chat.service.ChatSettingsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天模块基础设施配置类。
 * 主要职责是将 YAML/Properties 配置文件中的自定义配置项映射为类型安全的 Java 对象。
 */
@Configuration
// 激活 ChatSettingsProperties 类的属性绑定功能，使其可以被注入到其他 Service 中
@EnableConfigurationProperties(ChatSettingsProperties.class)
public class ChatConfig {

    /*
     * 该类目前作为配置入口，通过 @EnableConfigurationProperties 确保了
     * 整个聊天模块（Chat Module）能够共享统一的全局设置（如：默认模型、Token 限制等）。
     */
}