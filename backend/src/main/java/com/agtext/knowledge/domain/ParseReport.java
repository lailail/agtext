package com.agtext.knowledge.domain;

import java.time.Instant;

public record ParseReport(
    long id,
    long knowledgeDocumentId,
    Long jobId,
    String summary,
    Long extractedChars,
    Integer pageCount,
    Integer chunkCount,
    String parserName,
    String failedAt,
    String samplePreview,
    Instant createdAt,
    Instant updatedAt) {}
