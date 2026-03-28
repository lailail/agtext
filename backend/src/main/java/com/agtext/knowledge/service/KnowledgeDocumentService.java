package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.repository.KnowledgeDocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeDocumentService {
  private final KnowledgeDocumentRepository repo;

  public KnowledgeDocumentService(KnowledgeDocumentRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public KnowledgeDocument create(
      long knowledgeBaseId, String sourceType, String sourceUri, String title) {
    if (sourceType == null || sourceType.isBlank()) {
      throw new IllegalArgumentException("sourceType is required");
    }
    if (sourceUri == null || sourceUri.isBlank()) {
      throw new IllegalArgumentException("sourceUri is required");
    }
    long id = repo.create(knowledgeBaseId, sourceType.trim(), sourceUri.trim(), title);
    return repo.findById(id)
        .orElseThrow(() -> new IllegalStateException("Knowledge document not found"));
  }

  @Transactional(readOnly = true)
  public KnowledgeDocument get(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("DOC_NOT_FOUND", "Knowledge document not found"));
  }

  @Transactional(readOnly = true)
  public List<KnowledgeDocument> listByBaseId(long baseId, int page, int pageSize) {
    return repo.listByBaseId(baseId, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long countByBaseId(long baseId) {
    return repo.countByBaseId(baseId);
  }
}
