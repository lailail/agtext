package com.agtext.tool.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.tool.domain.ConfirmationItem;
import com.agtext.tool.repository.ConfirmationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmationService {
  private final ConfirmationRepository repo;

  public ConfirmationService(ConfirmationRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public ConfirmationItem create(
      String idempotencyKey,
      String actionType,
      String refType,
      String refId,
      String summary,
      String payload) {
    return repo.findByIdempotencyKey(idempotencyKey)
        .orElseGet(() -> repo.create(idempotencyKey, actionType, refType, refId, summary, payload));
  }

  @Transactional
  public void approve(long id) {
    get(id);
    repo.updateStatus(id, "approved");
  }

  @Transactional
  public void deny(long id) {
    get(id);
    repo.updateStatus(id, "denied");
  }

  @Transactional(readOnly = true)
  public ConfirmationItem get(long id) {
    return repo.findById(id)
        .orElseThrow(
            () -> new NotFoundException("CONFIRMATION_NOT_FOUND", "Confirmation not found"));
  }
}
