package com.agtext.chat.repository;

import com.agtext.chat.domain.Conversation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 会话持久化层。
 * 直接使用 JdbcTemplate 进行 SQL 操作，相比 ORM 框架，在大模型对话这种高频写入场景下具有更高的性能和更透明的执行计划。
 */
@Repository
public class ConversationRepository {
  private final JdbcTemplate jdbcTemplate;

  public ConversationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建新会话并获取数据库自动生成的自增 ID。
   * 使用 GeneratedKeyHolder 确保在插入后能立即拿到物理主键，以便后续关联消息。
   * @param title 会话初始标题
   * @return 生成的会话 ID
   */
  public long create(String title) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              // 指定需要回传的列名 "id"
              var ps =
                      connection.prepareStatement(
                              "insert into conversations(title, status) values (?, ?)", new String[] {"id"});
              ps.setString(1, title);
              ps.setString(2, "active"); // 初始状态硬编码为活跃
              return ps;
            },
            keyHolder);

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create conversation: Database did not return an ID");
    }
    return key.longValue();
  }

  /**
   * 按 ID 查询单条会话。
   * 返回 Optional 以便在 Service 层优雅处理 404 逻辑。
   */
  public Optional<Conversation> findById(long id) {
    List<Conversation> rows =
            jdbcTemplate.query("select * from conversations where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 基础的分页查询实现。
   * 采用 ID 倒序排列，确保用户最先看到最新的对话。
   * @param page     页码（1-based）
   * @param pageSize 每页条数
   */
  public List<Conversation> list(int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
            "select * from conversations order by id desc limit ? offset ?",
            mapper(),
            pageSize,
            offset);
  }

  /**
   * 统计全表记录数。
   * 用于前端分页组件计算总页数。
   */
  public long countAll() {
    Long total = jdbcTemplate.queryForObject("select count(*) from conversations", Long.class);
    return total == null ? 0 : total;
  }

  /**
   * 将数据库结果集（ResultSet）映射为领域模型对象。
   */
  private RowMapper<Conversation> mapper() {
    return (rs, rowNum) -> mapConversation(rs);
  }

  /**
   * 静态映射方法，负责 JDBC 类型到 Java 类型的转换（如 Timestamp 转 Instant）。
   */
  private static Conversation mapConversation(ResultSet rs) throws SQLException {
    return new Conversation(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("status"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}