package com.agtext.tool.repository;

import com.agtext.tool.domain.NotificationItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 通知数据仓库
 * 负责 notifications 表的增删改查及统计
 */
@Repository
public class NotificationRepository {
  private final JdbcTemplate jdbcTemplate;

  public NotificationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建通知并返回自增主键 ID
   */
  public long create(
          String type, String title, String content, String refType, String refId, String status) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              var ps =
                      connection.prepareStatement(
                              "insert into notifications(type, title, content, ref_type, ref_id, status) values (?,?,?,?,?,?)",
                              new String[] {"id"});
              ps.setString(1, type);
              ps.setString(2, title);
              ps.setString(3, content);
              ps.setString(4, refType);
              ps.setString(5, refId);
              ps.setString(6, status);
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create notification");
    }
    return key.longValue();
  }

  /**
   * 多条件动态分页查询通知列表
   * 排序规则：按 ID 倒序（即按创建时间降序）
   */
  public List<NotificationItem> list(String status, String type, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from notifications where 1=1");
    List<Object> args = new ArrayList<>();
    // 动态拼接过滤条件
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    if (type != null && !type.isBlank()) {
      sql.append(" and type=?");
      args.add(type);
    }
    sql.append(" order by id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  /**
   * 统计符合条件的通知总数，用于分页计算
   */
  public long count(String status, String type) {
    StringBuilder sql = new StringBuilder("select count(*) from notifications where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    if (type != null && !type.isBlank()) {
      sql.append(" and type=?");
      args.add(type);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  /**
   * 快速统计“未读”状态的通知数量
   * 常用于前端导航栏的小红点计数
   */
  public long countUnread() {
    Long total =
            jdbcTemplate.queryForObject(
                    "select count(*) from notifications where status='unread'", Long.class);
    return total == null ? 0 : total;
  }

  /**
   * 更新通知状态（如标记为 read 或 archived）
   */
  public void updateStatus(long id, String status) {
    jdbcTemplate.update("update notifications set status=? where id=?", status, id);
  }

  private RowMapper<NotificationItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 将数据库结果集映射为 NotificationItem 领域对象
   */
  private static NotificationItem map(ResultSet rs) throws SQLException {
    return new NotificationItem(
            rs.getLong("id"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("ref_type"),
            rs.getString("ref_id"),
            rs.getString("status"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}