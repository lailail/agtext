package com.agtext.memory.domain;

import java.time.Instant;

public record MemoryItem(
    long id,
    String title,
    String content,
    String status,
    String sourceType,
    Long sourceConversationId,
    Long sourceMessageId,
    Long relatedGoalId,
    Long relatedPlanId,
    Long relatedTaskId,
    String candidateReason,
    String reviewerNote,
    Instant reviewedAt,
    Instant approvedAt,
    Instant disabledAt,
    Instant createdAt,
    Instant updatedAt) {}
