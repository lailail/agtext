package com.agtext.chat.domain;

import java.time.Instant;

public record Conversation(
    long id, String title, String status, Instant createdAt, Instant updatedAt) {}
