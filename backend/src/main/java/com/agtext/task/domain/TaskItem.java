package com.agtext.task.domain;

import java.time.Instant;

public record TaskItem(
    long id,
    Long planId,
    Long goalId,
    boolean inbox,
    String title,
    String description,
    String status,
    int priority,
    Instant dueAt,
    Instant remindAt,
    Instant snoozeUntil,
    Instant createdAt,
    Instant updatedAt) {}
