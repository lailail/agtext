package com.agtext.knowledge.domain;

import java.time.Instant;

public record KnowledgeChunk(
    long id,
    long knowledgeDocumentId,
    Long importJobId,
    int chunkIndex,
    String content,
    String chunkHash,
    Instant createdAt,
    Instant updatedAt) {}
