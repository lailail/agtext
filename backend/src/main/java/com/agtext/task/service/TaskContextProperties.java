package com.agtext.task.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.task.context")
public record TaskContextProperties(boolean enabled, int maxTasksInPrompt) {}
