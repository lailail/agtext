package com.agtext.task.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.task.domain.GoalItem;
import com.agtext.task.repository.GoalRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
  private final GoalRepository repo;

  public GoalService(GoalRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public GoalItem create(String title, String description) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    long id = repo.create(title.trim(), description);
    return get(id);
  }

  @Transactional(readOnly = true)
  public GoalItem get(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("GOAL_NOT_FOUND", "Goal not found"));
  }

  @Transactional(readOnly = true)
  public List<GoalItem> list(String status, int page, int pageSize) {
    return repo.list(status, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long count(String status) {
    return repo.count(status);
  }

  @Transactional
  public GoalItem update(long id, String title, String description, String status) {
    get(id);
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title is required");
    }
    String st = status == null || status.isBlank() ? "active" : status;
    repo.update(id, title.trim(), description, st);
    return get(id);
  }
}
