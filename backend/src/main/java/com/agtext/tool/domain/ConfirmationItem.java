package com.agtext.tool.domain;

import java.time.Instant;

public record ConfirmationItem(
    long id,
    String status,
    String actionType,
    String refType,
    String refId,
    String summary,
    String payload,
    Instant createdAt,
    Instant updatedAt) {}
