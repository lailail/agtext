package com.agtext.task.service;

import com.agtext.common.ids.IdCodec;
import com.agtext.tool.service.NotificationService;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务提醒调度器
 * 职责：
 * 1. 定期轮询（Polling）数据库中已到达提醒时间（remindAt <= now）且未完成的任务。
 * 2. 确保提醒事件的幂等性（通过 tryRecordReminderEvent 避免重复推送）。
 * 3. 调用通知服务（NotificationService）向用户发送具体的提醒消息。
 */
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

  /**
   * 定期轮询到期提醒
   * 执行频率：由配置项 app.task.reminder.fixed-delay-ms 控制，默认 5 秒。
   * 事务说明：标记为 @Transactional 以确保扫描、记录事件和通知创建在逻辑上尽可能一致。
   */
  @Scheduled(fixedDelayString = "${app.task.reminder.fixed-delay-ms:5000}")
  @Transactional
  public void pollDueReminders() {
    // 1. 全局开关检查：如果配置中禁用了提醒功能，直接返回
    if (!props.enabled()) {
      return;
    }

    Instant now = Instant.now();
    // 2. 批量处理限制：防止单次扫描数据量过大导致 OOM 或数据库阻塞
    int limit = Math.max(1, Math.min(200, props.maxBatchSize()));

    // 3. 获取所有状态非“完成”或“取消”、且 remindAt <= 当前时间的任务
    List<com.agtext.task.domain.TaskItem> due = tasks.listReminderDue(now, limit);

    for (var t : due) {
      if (t.remindAt() == null) {
        continue;
      }

      // 4. 幂等性控制：尝试在 task_reminder_events 表中记录本次提醒
      // 如果该时间点的提醒已经记录过，则 tryRecordReminderEvent 会返回 false，跳过重复通知
      boolean recorded = tasks.tryRecordReminderEvent(t.id(), t.remindAt(), now);
      if (!recorded) {
        continue;
      }

      // 5. 构造通知内容
      String title = "任务提醒：" + t.title();
      String content = t.dueAt() == null
              ? "已到提醒时间。"
              : "已到提醒时间（到期：" + t.dueAt().toString() + "）。";

      // 6. 发送通知：关联任务业务类型及编码后的外部 ID
      notifications.create(
              "task_reminder",
              title,
              content,
              "task",
              IdCodec.encode(TASK_PREFIX, t.id()),
              "unread"
      );
    }
  }
}