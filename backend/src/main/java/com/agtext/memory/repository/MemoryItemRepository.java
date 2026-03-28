package com.agtext.memory.repository;

import com.agtext.memory.domain.MemoryItem;
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
 * 记忆条目数据访问层：
 * 使用原生 SQL 和 JdbcTemplate 进行高性能数据操作。
 */
@Repository
public class MemoryItemRepository {
  private final JdbcTemplate jdbcTemplate;

  public MemoryItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建初始状态为 'candidate' 的记忆条目
   * @return 数据库生成的自增 ID
   */
  public long createCandidate(
          String title,
          String content,
          String sourceType,
          Long sourceConversationId,
          Long sourceMessageId,
          String candidateReason) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              // 显式指定返回自增主键 "id"
              var ps =
                      connection.prepareStatement(
                              "insert into memory_items(title, content, status, source_type, source_conversation_id, source_message_id, candidate_reason) values (?,?,?,?,?,?,?)",
                              new String[] {"id"});
              ps.setString(1, title);
              ps.setString(2, content);
              ps.setString(3, "candidate");
              ps.setString(4, sourceType);
              ps.setObject(5, sourceConversationId);
              ps.setObject(6, sourceMessageId);
              ps.setString(7, candidateReason);
              return ps;
            },
            keyHolder);

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create memory item");
    }
    return key.longValue();
  }

  /**
   * 根据 ID 查询单条记录
   */
  public Optional<MemoryItem> findById(long id) {
    List<MemoryItem> rows =
            jdbcTemplate.query("select * from memory_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 分页查询
   * @param status 可选的状态过滤条件
   */
  public List<MemoryItem> list(String status, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from memory_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    sql.append(" order by id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  /**
   * 统计特定状态下的条目总数，用于分页计算
   */
  public long count(String status) {
    StringBuilder sql = new StringBuilder("select count(*) from memory_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  /**
   * 审核通过：同步更新 status, reviewer_note, reviewed_at 和 approved_at
   */
  public void approve(long id, String reviewerNote) {
    jdbcTemplate.update(
            "update memory_items set status='approved', reviewer_note=?, reviewed_at=current_timestamp, approved_at=current_timestamp where id=?",
            reviewerNote,
            id);
  }

  /**
   * 禁用条目：记录禁用时间戳
   */
  public void disable(long id, String reviewerNote) {
    jdbcTemplate.update(
            "update memory_items set status='disabled', reviewer_note=?, reviewed_at=current_timestamp, disabled_at=current_timestamp where id=?",
            reviewerNote,
            id);
  }

  /**
   * 修改记忆标题与内容
   */
  public void updateContent(long id, String title, String content) {
    jdbcTemplate.update(
            "update memory_items set title=?, content=? where id=?", title, content, id);
  }

  /**
   * 更新记忆与业务对象（目标、计划、任务）的关联关系
   */
  public void updateLinks(long id, Long relatedGoalId, Long relatedPlanId, Long relatedTaskId) {
    jdbcTemplate.update(
            "update memory_items set related_goal_id=?, related_plan_id=?, related_task_id=? where id=?",
            relatedGoalId,
            relatedPlanId,
            relatedTaskId,
            id);
  }

  /**
   * 获取已生效的记忆条目，通常用于 RAG 检索时的全量或增量加载
   */
  public List<MemoryItem> listApproved(int limit) {
    return jdbcTemplate.query(
            "select * from memory_items where status='approved' order by approved_at desc, id desc limit ?",
            mapper(),
            limit);
  }

  /**
   * 定义从结果集到领域对象的映射规则
   */
  private RowMapper<MemoryItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集手动映射：
   * 显式处理 Nullable 字段（使用 getObject 转 Long）和时间戳转换。
   */
  private static MemoryItem map(ResultSet rs) throws SQLException {
    return new MemoryItem(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("status"),
            rs.getString("source_type"),
            (Long) rs.getObject("source_conversation_id"),
            (Long) rs.getObject("source_message_id"),
            (Long) rs.getObject("related_goal_id"),
            (Long) rs.getObject("related_plan_id"),
            (Long) rs.getObject("related_task_id"),
            rs.getString("candidate_reason"),
            rs.getString("reviewer_note"),
            toInstant(rs.getTimestamp("reviewed_at")),
            toInstant(rs.getTimestamp("approved_at")),
            toInstant(rs.getTimestamp("disabled_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  /**
   * Timestamp 转 Instant，处理数据库 null 值
   */
  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}