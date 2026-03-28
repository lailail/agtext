package com.agtext.task.service;

import com.agtext.common.ids.IdCodec;
import com.agtext.task.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskContextService {
  private static final String TASK_PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";

  private final TaskRepository repo;

  public TaskContextService(TaskRepository repo) {
    this.repo = repo;
  }

  @Transactional(readOnly = true)
  public String buildSystemContext(int maxTasks) {
    int limit = Math.max(1, Math.min(50, maxTasks));
    List<TaskContextItem> tasks = repo.listForChatContext(limit);
    if (tasks.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("当前进行中的任务（供参考；如用户提到相关事项，可结合任务给出下一步建议）：\n");
    for (TaskContextItem t : tasks) {
      sb.append("- [")
          .append(t.status())
          .append("] ")
          .append(t.title())
          .append(" (id=")
          .append(IdCodec.encode(TASK_PREFIX, t.id()))
          .append(")");
      if (t.dueAt() != null) {
        sb.append(" dueAt=").append(t.dueAt());
      }
      if (t.planId() != null) {
        sb.append(" planId=").append(IdCodec.encode(PLAN_PREFIX, t.planId()));
      }
      if (t.goalId() != null) {
        sb.append(" goalId=").append(IdCodec.encode(GOAL_PREFIX, t.goalId()));
      }
      if (t.inbox()) {
        sb.append(" inbox=true");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public record TaskContextItem(
      long id,
      Long planId,
      Long goalId,
      boolean inbox,
      String title,
      String status,
      Instant dueAt) {}
}
