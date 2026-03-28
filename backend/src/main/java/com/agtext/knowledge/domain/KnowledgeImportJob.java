package com.agtext.knowledge.domain;

import java.time.Instant;

public record KnowledgeImportJob(
    long id,
    long knowledgeBaseId,
    Long documentId,
    String status,
    Instant startedAt,
    Instant finishedAt,
    String stage,
    Integer progress,
    String errorMessage,
    Instant cancelledAt,
    Instant createdAt,
    Instant updatedAt) {}
