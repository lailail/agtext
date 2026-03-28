package com.agtext.task.service;

import com.agtext.common.ids.IdCodec;
import com.agtext.tool.service.NotificationService;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TaskReminderScheduler {
  private static final String TASK_PREFIX = "task_";

  private final TaskService tasks;
  private final NotificationService notifications;
  private final TaskReminderProperties props;

  public TaskReminderScheduler(
      TaskService tasks, NotificationService notifications, TaskReminderProperties props) {
    this.tasks = tasks;
    this.notifications = notifications;
    this.props = props;
  }

  @Scheduled(fixedDelayString = "${app.task.reminder.fixed-delay-ms:5000}")
  @Transactional
  public void pollDueReminders() {
    if (!props.enabled()) {
      return;
    }
    Instant now = Instant.now();
    int limit = Math.max(1, Math.min(200, props.maxBatchSize()));
    List<com.agtext.task.domain.TaskItem> due = tasks.listReminderDue(now, limit);
    for (var t : due) {
      if (t.remindAt() == null) {
        continue;
      }
      boolean recorded = tasks.tryRecordReminderEvent(t.id(), t.remindAt(), now);
      if (!recorded) {
        continue;
      }
      String title = "任务提醒：" + t.title();
      String content = t.dueAt() == null ? "已到提醒时间。" : "已到提醒时间（到期：" + t.dueAt().toString() + "）。";
      notifications.create(
          "task_reminder", title, content, "task", IdCodec.encode(TASK_PREFIX, t.id()), "unread");
    }
  }
}
