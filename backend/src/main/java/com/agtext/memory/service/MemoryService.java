package com.agtext.memory.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.memory.domain.MemoryItem;
import com.agtext.memory.repository.MemoryItemRepository;
import com.agtext.task.service.GoalService;
import com.agtext.task.service.PlanService;
import com.agtext.task.service.TaskService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemoryService {
  private final MemoryItemRepository repo;
  private final GoalService goals;
  private final PlanService plans;
  private final TaskService tasks;

  public MemoryService(
      MemoryItemRepository repo, GoalService goals, PlanService plans, TaskService tasks) {
    this.repo = repo;
    this.goals = goals;
    this.plans = plans;
    this.tasks = tasks;
  }

  @Transactional
  public MemoryItem createCandidate(
      String title,
      String content,
      String sourceType,
      Long sourceConversationId,
      Long sourceMessageId,
      String candidateReason) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content is required");
    }
    long id =
        repo.createCandidate(
            title == null || title.isBlank() ? null : title.trim(),
            content.trim(),
            sourceType,
            sourceConversationId,
            sourceMessageId,
            candidateReason);
    return get(id);
  }

  @Transactional(readOnly = true)
  public MemoryItem get(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("MEMORY_NOT_FOUND", "Memory item not found"));
  }

  @Transactional(readOnly = true)
  public List<MemoryItem> list(String status, int page, int pageSize) {
    return repo.list(status, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long count(String status) {
    return repo.count(status);
  }

  @Transactional
  public MemoryItem approve(long id, String reviewerNote) {
    get(id);
    repo.approve(id, reviewerNote);
    return get(id);
  }

  @Transactional
  public MemoryItem disable(long id, String reviewerNote) {
    get(id);
    repo.disable(id, reviewerNote);
    return get(id);
  }

  @Transactional
  public MemoryItem update(long id, String title, String content) {
    get(id);
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content is required");
    }
    repo.updateContent(id, title, content.trim());
    return get(id);
  }

  @Transactional
  public MemoryItem linkToTask(
      long id, Long relatedGoalId, Long relatedPlanId, Long relatedTaskId) {
    get(id);
    if (relatedGoalId != null) {
      goals.get(relatedGoalId);
    }
    if (relatedPlanId != null) {
      plans.get(relatedPlanId);
    }
    if (relatedTaskId != null) {
      tasks.get(relatedTaskId);
    }
    repo.updateLinks(id, relatedGoalId, relatedPlanId, relatedTaskId);
    return get(id);
  }

  @Transactional(readOnly = true)
  public List<MemoryItem> listApproved(int limit) {
    return repo.listApproved(limit);
  }
}
