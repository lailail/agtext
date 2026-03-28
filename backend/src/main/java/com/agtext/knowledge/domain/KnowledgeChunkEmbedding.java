package com.agtext.knowledge.domain;

import java.time.Instant;

public record KnowledgeChunkEmbedding(
    long id,
    long knowledgeChunkId,
    long importJobId,
    String provider,
    String model,
    Integer dim,
    String vectorJson,
    Instant createdAt,
    Instant updatedAt) {}
