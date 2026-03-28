package com.agtext.tool.repository;

import com.agtext.tool.domain.ExecutionRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 执行记录数据仓库
 * 负责审计日志 execution_records 的持久化与分页查询
 */
@Repository
public class ExecutionRecordRepository {
  private final JdbcTemplate jdbcTemplate;

  public ExecutionRecordRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 插入执行记录
   * 注意：SQL 中为了兼容旧版表结构，对部分含义相近的字段进行了重复写入（如 actor 和 initiator）
   */
  public void create(
          String actor,
          String source,
          String actionType,
          String refType,
          String refId,
          String idempotencyKey,
          String inputSummary,
          String outputSummary,
          String status,
          String errorCode,
          Long durationMs) {
    jdbcTemplate.update(
            "insert into execution_records(initiator, ref_type, ref_id, input_summary, output_summary, duration_ms, result, failure_reason, actor, source, action_type, idempotency_key, status, error_code) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            actor,           // 映射到 initiator
            refType,
            refId,
            inputSummary,
            outputSummary,
            durationMs,
            status,          // 映射到 result
            errorCode,       // 映射到 failure_reason
            actor,           // 映射到新版字段 actor
            source,
            actionType,
            idempotencyKey,
            status,          // 映射到新版字段 status
            errorCode);      // 映射到新版字段 error_code
  }

  /**
   * 按关联资源（refType/refId）分页查询执行轨迹
   * 排序规则：按 ID 倒序（最新记录在前）
   */
  public List<ExecutionRecord> list(String refType, String refId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from execution_records where 1=1");
    List<Object> args = new ArrayList<>();
    // 动态拼接过滤条件
    if (refType != null && !refType.isBlank()) {
      sql.append(" and ref_type=?");
      args.add(refType);
    }
    if (refId != null && !refId.isBlank()) {
      sql.append(" and ref_id=?");
      args.add(refId);
    }
    sql.append(" order by id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  /**
   * 统计符合条件的执行记录总数
   */
  public long count(String refType, String refId) {
    StringBuilder sql = new StringBuilder("select count(*) from execution_records where 1=1");
    List<Object> args = new ArrayList<>();
    if (refType != null && !refType.isBlank()) {
      sql.append(" and ref_type=?");
      args.add(refType);
    }
    if (refId != null && !refId.isBlank()) {
      sql.append(" and ref_id=?");
      args.add(refId);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  private RowMapper<ExecutionRecord> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集映射逻辑
   * 采用了 coalesce 策略处理数据库字段迁移期间的空值问题
   */
  private static ExecutionRecord map(ResultSet rs) throws SQLException {
    Long duration = (Long) rs.getObject("duration_ms");
    return new ExecutionRecord(
            rs.getLong("id"),
            // 优先取 actor，不存在则回退取 initiator
            coalesce(rs.getString("actor"), rs.getString("initiator")),
            rs.getString("source"),
            rs.getString("action_type"),
            rs.getString("ref_type"),
            rs.getString("ref_id"),
            rs.getString("idempotency_key"),
            rs.getString("input_summary"),
            rs.getString("output_summary"),
            // 优先取 status，不存在则回退取 result
            coalesce(rs.getString("status"), rs.getString("result")),
            rs.getString("error_code"),
            duration,
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  /**
   * 简单的空值回退工具方法
   */
  private static String coalesce(String a, String b) {
    return a == null || a.isBlank() ? b : a;
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}