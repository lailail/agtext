package com.agtext.task.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.task.domain.PlanItem;
import com.agtext.task.repository.PlanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {
  private final PlanRepository repo;
  private final GoalService goals;

  public PlanService(PlanRepository repo, GoalService goals) {
    this.repo = repo;
    this.goals = goals;
  }

  @Transactional
  public PlanItem create(Long goalId, String title, String description) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    if (goalId != null) {
      goals.get(goalId);
    }
    long id = repo.create(goalId, title.trim(), description);
    return get(id);
  }

  @Transactional(readOnly = true)
  public PlanItem get(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("PLAN_NOT_FOUND", "Plan not found"));
  }

  @Transactional(readOnly = true)
  public List<PlanItem> list(Long goalId, String status, int page, int pageSize) {
    return repo.list(goalId, status, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long count(Long goalId, String status) {
    return repo.count(goalId, status);
  }

  @Transactional
  public PlanItem update(long id, Long goalId, String title, String description, String status) {
    get(id);
    if (goalId != null) {
      goals.get(goalId);
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    String st = status == null || status.isBlank() ? "active" : status;
    repo.update(id, goalId, title.trim(), description, st);
    return get(id);
  }
}
