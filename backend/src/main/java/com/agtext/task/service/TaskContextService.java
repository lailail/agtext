package com.agtext.task.service;

import com.agtext.common.ids.IdCodec;
import com.agtext.task.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务上下文服务
 * 职责：
 * 1. 提取当前活跃（进行中）的任务信息。
 * 2. 将任务列表格式化为适合 AI 提示词（Prompt）注入的字符串。
 * 3. 确保在提示词中输出的是经过编码的外部 ID（混淆后的 ID），以便 AI 回复时能引用正确的标识。
 */
@Service
public class TaskContextService {
  private static final String TASK_PREFIX = "task_";
  private static final String PLAN_PREFIX = "plan_";
  private static final String GOAL_PREFIX = "goal_";

  private final TaskRepository repo;

  public TaskContextService(TaskRepository repo) {
    this.repo = repo;
  }

  /**
   * 构建系统层面的上下文字符串
   * @param maxTasks 允许注入的最大任务条数
   * @return 格式化后的任务列表文本，若无任务则返回空字符串
   */
  @Transactional(readOnly = true)
  public String buildSystemContext(int maxTasks) {
    // 强制限制提取数量，防止 Context 过长导致 AI 响应延迟或 Token 溢出
    int limit = Math.max(1, Math.min(50, maxTasks));

    // 调用 Repository 专门为 Chat 设计的轻量级查询
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
              .append(IdCodec.encode(TASK_PREFIX, t.id())) // 关键：对 AI 暴露编码后的 ID
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

  /**
   * 任务上下文投影对象
   * 仅包含 AI 需要感知的基础属性，减少内存开销。
   */
  public record TaskContextItem(
          long id,
          Long planId,
          Long goalId,
          boolean inbox,
          String title,
          String status,
          Instant dueAt) {}
}