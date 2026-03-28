package com.agtext.task.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.task.reminder")
public record TaskReminderProperties(boolean enabled, int maxBatchSize, long fixedDelayMs) {}
