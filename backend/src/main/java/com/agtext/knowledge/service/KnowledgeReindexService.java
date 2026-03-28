package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.domain.KnowledgeImportJob;
import com.agtext.knowledge.repository.KnowledgeImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeReindexService {
  private final KnowledgeDocumentService documents;
  private final KnowledgeImportJobRepository jobs;
  private final KnowledgeEmbeddingIndexService indexer;

  public KnowledgeReindexService(
      KnowledgeDocumentService documents,
      KnowledgeImportJobRepository jobs,
      KnowledgeEmbeddingIndexService indexer) {
    this.documents = documents;
    this.jobs = jobs;
    this.indexer = indexer;
  }

  @Transactional
  public KnowledgeImportJob reindex(long documentId) {
    KnowledgeDocument doc = documents.get(documentId);
    Long sourceChunkJobId =
        doc.latestImportJobId() != null ? doc.latestImportJobId() : doc.activeImportJobId();
    if (sourceChunkJobId == null) {
      throw new NotFoundException("DOC_NO_CONTENT", "Document has no imported content to reindex");
    }

    long jobId = jobs.create(doc.knowledgeBaseId(), doc.id());
    try {
      indexer.buildIndex(jobId, doc.id(), sourceChunkJobId);
    } catch (RuntimeException e) {
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      jobs.markFailed(jobId, "index", msg);
    }
    return jobs.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }
}
