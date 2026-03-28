package com.agtext.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.task.service.TaskReminderScheduler;
import com.agtext.task.service.TaskService;
import com.agtext.tool.service.NotificationService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {"app.task.reminder.enabled=true", "app.task.reminder.fixed-delay-ms=600000"})
@ActiveProfiles("test")
class TaskReminderSchedulerIntegrationTest {
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TaskService tasks;
  @Autowired private TaskReminderScheduler scheduler;
  @Autowired private NotificationService notifications;

  @BeforeEach
  void resetTables() {
    jdbc.update("delete from notifications");
    jdbc.update("delete from task_reminder_events");
    jdbc.update("delete from memory_items");
    jdbc.update("delete from task_items");
  }

  @Test
  void shouldBeIdempotentForSameTaskAndRemindAt() {
    Instant remindAt = Instant.now().minusSeconds(30);
    tasks.create(null, null, false, "t1", null, "todo", 0, null, remindAt);

    assertThat(notifications.count(null, "task_reminder")).isEqualTo(0);

    scheduler.pollDueReminders();
    assertThat(notifications.count(null, "task_reminder")).isEqualTo(1);

    scheduler.pollDueReminders();
    assertThat(notifications.count(null, "task_reminder")).isEqualTo(1);
  }
}
