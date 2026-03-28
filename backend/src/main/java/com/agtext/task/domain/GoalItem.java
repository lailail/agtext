package com.agtext.task.domain;

import java.time.Instant;

public record GoalItem(
    long id,
    String title,
    String description,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
