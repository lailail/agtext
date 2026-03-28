package com.agtext.knowledge.domain;

import java.time.Instant;

public record KnowledgeBase(
    long id, String name, String description, Instant createdAt, Instant updatedAt) {}
