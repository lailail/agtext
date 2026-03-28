package com.agtext.chat.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 聊天业务配置类：用于映射 application.yml 或 application.properties 中以 "app.chat" 为前缀的配置项
 */
@ConfigurationProperties(prefix = "app.chat")
public record ChatSettingsProperties(
        // 全局系统提示词 (System Prompt)，用于初始化大模型的人格、语言风格及行为约束
        String systemPrompt,
        // 历史消息加载上限，用于控制单次对话请求中携带的上下文长度，以平衡模型记忆力与 Token 消耗
        int maxHistoryMessages) {

}