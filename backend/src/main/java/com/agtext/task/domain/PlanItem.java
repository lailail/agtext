package com.agtext.task.domain;

import java.time.Instant;

public record PlanItem(
    long id,
    Long goalId,
    String title,
    String description,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
