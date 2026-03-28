package com.agtext.knowledge.domain;

import java.time.Instant;

public record KnowledgeDocument(
    long id,
    long knowledgeBaseId,
    String sourceType,
    String sourceUri,
    String title,
    String status,
    String parseStatus,
    String indexStatus,
    String errorMessage,
    String contentHash,
    Long latestImportJobId,
    Long activeImportJobId,
    Instant createdAt,
    Instant updatedAt) {}
