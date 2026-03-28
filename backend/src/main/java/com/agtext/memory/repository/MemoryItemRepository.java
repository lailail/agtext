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

@Repository
public class MemoryItemRepository {
  private final JdbcTemplate jdbcTemplate;

  public MemoryItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

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

  public Optional<MemoryItem> findById(long id) {
    List<MemoryItem> rows =
        jdbcTemplate.query("select * from memory_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

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

  public void approve(long id, String reviewerNote) {
    jdbcTemplate.update(
        "update memory_items set status='approved', reviewer_note=?, reviewed_at=current_timestamp, approved_at=current_timestamp where id=?",
        reviewerNote,
        id);
  }

  public void disable(long id, String reviewerNote) {
    jdbcTemplate.update(
        "update memory_items set status='disabled', reviewer_note=?, reviewed_at=current_timestamp, disabled_at=current_timestamp where id=?",
        reviewerNote,
        id);
  }

  public void updateContent(long id, String title, String content) {
    jdbcTemplate.update(
        "update memory_items set title=?, content=? where id=?", title, content, id);
  }

  public void updateLinks(long id, Long relatedGoalId, Long relatedPlanId, Long relatedTaskId) {
    jdbcTemplate.update(
        "update memory_items set related_goal_id=?, related_plan_id=?, related_task_id=? where id=?",
        relatedGoalId,
        relatedPlanId,
        relatedTaskId,
        id);
  }

  public List<MemoryItem> listApproved(int limit) {
    return jdbcTemplate.query(
        "select * from memory_items where status='approved' order by approved_at desc, id desc limit ?",
        mapper(),
        limit);
  }

  private RowMapper<MemoryItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

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

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
