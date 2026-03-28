package com.agtext.task.repository;

import com.agtext.task.domain.TaskItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 任务数据访问层
 * 负责 task_items 表的增删改查及复杂过滤逻辑
 */
@Repository
public class TaskRepository {
  private final JdbcTemplate jdbcTemplate;

  public TaskRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建新任务并返回自增主键 ID
   */
  public long create(
          Long planId,
          Long goalId,
          boolean inbox,
          String title,
          String description,
          String status,
          int priority,
          Instant dueAt,
          Instant remindAt,
          Instant snoozeUntil) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              // 使用 prepareStatement 指定返回自增主键 "id"
              var ps =
                      connection.prepareStatement(
                              "insert into task_items(plan_id, goal_id, inbox, title, description, status, priority, due_at, remind_at, snooze_until) values (?,?,?,?,?,?,?,?,?,?)",
                              new String[] {"id"});
              ps.setObject(1, planId);
              ps.setObject(2, goalId);
              ps.setBoolean(3, inbox);
              ps.setString(4, title);
              ps.setString(5, description);
              ps.setString(6, status);
              ps.setInt(7, priority);
              // 时间戳转换逻辑：Instant 转 Timestamp
              ps.setTimestamp(8, dueAt == null ? null : Timestamp.from(dueAt));
              ps.setTimestamp(9, remindAt == null ? null : Timestamp.from(remindAt));
              ps.setTimestamp(10, snoozeUntil == null ? null : Timestamp.from(snoozeUntil));
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create task");
    }
    return key.longValue();
  }

  /**
   * 根据 ID 查询单个任务详情
   */
  public Optional<TaskItem> findById(long id) {
    List<TaskItem> rows = jdbcTemplate.query("select * from task_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 多条件动态分页查询任务列表
   * 排序规则：优先级降序 > 截止时间升序 > ID 降序
   */
  public List<TaskItem> list(
          Long planId,
          Long goalId,
          Boolean inbox,
          String status,
          Instant dueBefore,
          int page,
          int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from task_items where 1=1");
    List<Object> args = new ArrayList<>();
    // 动态拼接 SQL 条件
    if (planId != null) {
      sql.append(" and plan_id=?");
      args.add(planId);
    }
    if (goalId != null) {
      sql.append(" and goal_id=?");
      args.add(goalId);
    }
    if (inbox != null) {
      sql.append(" and inbox=?");
      args.add(inbox);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    if (dueBefore != null) {
      sql.append(" and due_at is not null and due_at<=?");
      args.add(Timestamp.from(dueBefore));
    }
    sql.append(" order by priority desc, due_at asc, id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  /**
   * 统计符合条件的任务总数
   */
  public long count(Long planId, Long goalId, Boolean inbox, String status, Instant dueBefore) {
    StringBuilder sql = new StringBuilder("select count(*) from task_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (planId != null) {
      sql.append(" and plan_id=?");
      args.add(planId);
    }
    if (goalId != null) {
      sql.append(" and goal_id=?");
      args.add(goalId);
    }
    if (inbox != null) {
      sql.append(" and inbox=?");
      args.add(inbox);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    if (dueBefore != null) {
      sql.append(" and due_at is not null and due_at<=?");
      args.add(Timestamp.from(dueBefore));
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  /**
   * 全量更新任务信息
   */
  public void update(
          long id,
          Long planId,
          Long goalId,
          boolean inbox,
          String title,
          String description,
          String status,
          int priority,
          Instant dueAt,
          Instant remindAt,
          Instant snoozeUntil) {
    jdbcTemplate.update(
            "update task_items set plan_id=?, goal_id=?, inbox=?, title=?, description=?, status=?, priority=?, due_at=?, remind_at=?, snooze_until=? where id=?",
            planId,
            goalId,
            inbox,
            title,
            description,
            status,
            priority,
            dueAt == null ? null : Timestamp.from(dueAt),
            remindAt == null ? null : Timestamp.from(remindAt),
            snoozeUntil == null ? null : Timestamp.from(snoozeUntil),
            id);
  }

  /**
   * 仅更新提醒时间与稍后提醒时间
   */
  public void updateReminder(long id, Instant remindAt, Instant snoozeUntil) {
    jdbcTemplate.update(
            "update task_items set remind_at=?, snooze_until=? where id=?",
            remindAt == null ? null : Timestamp.from(remindAt),
            snoozeUntil == null ? null : Timestamp.from(snoozeUntil),
            id);
  }

  /**
   * 更新任务状态并清空提醒设置（常用于任务完成或取消）
   */
  public void updateStatusAndClearReminder(long id, String status) {
    jdbcTemplate.update(
            "update task_items set status=?, remind_at=null, snooze_until=null where id=?", status, id);
  }

  /**
   * 查询到达提醒时间且状态非“已完成/已取消”的任务
   */
  public List<TaskItem> listRemindDue(Instant now, int limit) {
    int capped = Math.max(1, Math.min(200, limit));
    return jdbcTemplate.query(
            "select * from task_items where remind_at is not null and remind_at<=? and status not in ('done','cancelled') order by remind_at asc, id asc limit ?",
            mapper(),
            Timestamp.from(now),
            capped);
  }

  /**
   * 记录提醒触发事件到日志表
   */
  public int insertReminderEvent(
          long taskId, Instant remindAt, String type, String status, Instant firedAt) {
    return jdbcTemplate.update(
            "insert into task_reminder_events(task_item_id, remind_at, type, status, fired_at) values (?,?,?,?,?)",
            taskId,
            Timestamp.from(remindAt),
            type,
            status,
            Timestamp.from(firedAt));
  }

  /**
   * 获取指定时间范围 [from, to) 内到期的未完成任务
   */
  public List<TaskItem> listDueBetween(Instant from, Instant to, int limit) {
    int capped = Math.max(1, Math.min(200, limit));
    return jdbcTemplate.query(
            "select * from task_items where due_at is not null and due_at>=? and due_at<? and status not in ('done','cancelled') order by due_at asc, id asc limit ?",
            mapper(),
            Timestamp.from(from),
            Timestamp.from(to),
            capped);
  }

  /**
   * 获取截止时间已过（过期）的未完成任务
   */
  public List<TaskItem> listOverdue(Instant now, int limit) {
    int capped = Math.max(1, Math.min(200, limit));
    return jdbcTemplate.query(
            "select * from task_items where due_at is not null and due_at<? and status not in ('done','cancelled') order by due_at asc, id asc limit ?",
            mapper(),
            Timestamp.from(now),
            capped);
  }

  /**
   * 查询指定时间范围 [from, to) 内需要触发提醒的未完成任务
   */
  public List<TaskItem> listRemindBetween(Instant from, Instant to, int limit) {
    int capped = Math.max(1, Math.min(200, limit));
    return jdbcTemplate.query(
            "select * from task_items where remind_at is not null and remind_at>=? and remind_at<? and status not in ('done','cancelled') order by remind_at asc, id asc limit ?",
            mapper(),
            Timestamp.from(from),
            Timestamp.from(to),
            capped);
  }

  /**
   * 将收件箱任务归档（移除 inbox 标记并关联计划/目标）
   */
  public void archiveInboxItem(long id, Long planId, Long goalId) {
    jdbcTemplate.update(
            "update task_items set inbox=false, plan_id=?, goal_id=? where id=?", planId, goalId, id);
  }

  /**
   * 逻辑删除/取消收件箱任务
   */
  public void deleteInboxItem(long id) {
    jdbcTemplate.update(
            "update task_items set inbox=false, status='cancelled', remind_at=null, snooze_until=null where id=?",
            id);
  }

  /**
   * 获取用于 AI 聊天上下文的任务简要信息列表
   * 仅包含未完成的任务（todo, in_progress）
   */
  public List<com.agtext.task.service.TaskContextService.TaskContextItem> listForChatContext(
          int limit) {
    int capped = Math.max(1, Math.min(50, limit));
    return jdbcTemplate.query(
            "select id, plan_id, goal_id, inbox, title, status, due_at from task_items where status in ('todo','in_progress') order by priority desc, due_at asc, id desc limit ?",
            (rs, rowNum) ->
                    new com.agtext.task.service.TaskContextService.TaskContextItem(
                            rs.getLong("id"),
                            (Long) rs.getObject("plan_id"),
                            (Long) rs.getObject("goal_id"),
                            rs.getBoolean("inbox"),
                            rs.getString("title"),
                            rs.getString("status"),
                            toInstant(rs.getTimestamp("due_at"))),
            capped);
  }

  private RowMapper<TaskItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 将数据库结果集映射为 TaskItem 实体对象
   */
  private static TaskItem map(ResultSet rs) throws SQLException {
    return new TaskItem(
            rs.getLong("id"),
            (Long) rs.getObject("plan_id"),
            (Long) rs.getObject("goal_id"),
            rs.getBoolean("inbox"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getInt("priority"),
            toInstant(rs.getTimestamp("due_at")),
            toInstant(rs.getTimestamp("remind_at")),
            toInstant(rs.getTimestamp("snooze_until")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  /**
   * 安全转换 Timestamp 到 Instant，处理 null 值
   */
  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}