package com.agtext.tool.domain;

import java.time.Instant;

public record ExecutionRecord(
    long id,
    String actor,
    String source,
    String actionType,
    String refType,
    String refId,
    String idempotencyKey,
    String inputSummary,
    String outputSummary,
    String status,
    String errorCode,
    Long durationMs,
    Instant createdAt,
    Instant updatedAt) {}
