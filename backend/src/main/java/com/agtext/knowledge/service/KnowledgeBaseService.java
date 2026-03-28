package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeBase;
import com.agtext.knowledge.repository.KnowledgeBaseRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeBaseService {
  private final KnowledgeBaseRepository repo;

  public KnowledgeBaseService(KnowledgeBaseRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public KnowledgeBase create(String name, String description) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    long id;
    try {
      id = repo.create(name.trim(), description);
    } catch (DuplicateKeyException e) {
      throw new IllegalArgumentException("knowledge base name already exists");
    }
    return repo.findById(id)
        .orElseThrow(() -> new IllegalStateException("Knowledge base not found"));
  }

  @Transactional(readOnly = true)
  public KnowledgeBase get(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("KB_NOT_FOUND", "Knowledge base not found"));
  }

  @Transactional(readOnly = true)
  public List<KnowledgeBase> list(int page, int pageSize) {
    return repo.list(page, pageSize);
  }

  @Transactional(readOnly = true)
  public long countAll() {
    return repo.countAll();
  }
}
