package com.agtext.chat.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat")
public record ChatSettingsProperties(String systemPrompt, int maxHistoryMessages) {}
