package com.agtext.task.service;

import com.agtext.task.domain.PlanItem;
import com.agtext.task.domain.TaskItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanAutomationService {
  private final PlanDecompositionService decomposer;
  private final PlanService plans;
  private final TaskService tasks;

  public PlanAutomationService(
      PlanDecompositionService decomposer, PlanService plans, TaskService tasks) {
    this.decomposer = decomposer;
    this.plans = plans;
    this.tasks = tasks;
  }

  public PlanDecompositionService.DecompositionResult decompose(
      String objective, String provider, String model, Integer maxSteps) {
    int limit = maxSteps == null ? 8 : maxSteps;
    return decomposer.decompose(objective, provider, model, limit);
  }

  @Transactional
  public PlanWithTasks createFromObjective(
      Long goalId, String objective, String provider, String model, Integer maxSteps) {
    PlanDecompositionService.DecompositionResult r =
        decompose(objective, provider, model, maxSteps);
    String title = r.title();
    if (title == null || title.isBlank()) {
      title = defaultTitle(objective);
    }
    PlanItem plan = plans.create(goalId, title, objective);

    List<TaskItem> created = new ArrayList<>();
    for (var s : r.steps()) {
      TaskItem t =
          tasks.create(plan.id(), goalId, false, s.title(), s.description(), "todo", 0, null, null);
      created.add(t);
    }
    return new PlanWithTasks(plan, created);
  }

  private static String defaultTitle(String objective) {
    if (objective == null) {
      return "Plan";
    }
    String t = objective.trim();
    if (t.length() <= 32) {
      return t;
    }
    return t.substring(0, 32);
  }

  public record PlanWithTasks(PlanItem plan, List<TaskItem> tasks) {}
}
