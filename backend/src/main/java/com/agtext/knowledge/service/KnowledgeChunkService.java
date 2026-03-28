package com.agtext.knowledge.service;

import com.agtext.knowledge.domain.KnowledgeChunk;
import com.agtext.knowledge.repository.KnowledgeChunkRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeChunkService {
  private final KnowledgeChunkRepository repo;

  public KnowledgeChunkService(KnowledgeChunkRepository repo) {
    this.repo = repo;
  }

  @Transactional(readOnly = true)
  public List<KnowledgeChunk> listByDocumentId(long documentId, int page, int pageSize) {
    return repo.listByDocumentId(documentId, page, pageSize);
  }

  @Transactional(readOnly = true)
  public List<KnowledgeChunk> listByDocumentIdAndImportJobId(
      long documentId, long importJobId, int page, int pageSize) {
    return repo.listByDocumentIdAndImportJobId(documentId, importJobId, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long countByDocumentId(long documentId) {
    return repo.countByDocumentId(documentId);
  }

  @Transactional(readOnly = true)
  public long countByDocumentIdAndImportJobId(long documentId, long importJobId) {
    return repo.countByDocumentIdAndImportJobId(documentId, importJobId);
  }
}
