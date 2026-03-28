package com.agtext.chat.domain;

import java.time.Instant;

public record Message(
    long id,
    long conversationId,
    String role,
    String content,
    String provider,
    String modelName,
    Integer tokens,
    Instant createdAt,
    Instant updatedAt) {}
