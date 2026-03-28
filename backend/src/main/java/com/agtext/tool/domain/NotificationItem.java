package com.agtext.tool.domain;

import java.time.Instant;

public record NotificationItem(
    long id,
    String type,
    String title,
    String content,
    String refType,
    String refId,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
