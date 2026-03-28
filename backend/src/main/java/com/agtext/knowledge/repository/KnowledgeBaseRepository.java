package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeBase;
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
 * 知识库持久层：负责 knowledge_bases 表的底层 SQL 操作。
 * 使用 Spring JdbcTemplate 进行数据访问，确保资源的自动管理与异常转换。
 */
@Repository
public class KnowledgeBaseRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeBaseRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建知识库记录并返回自增主键。
   * 使用 KeyHolder 捕获数据库生成的 ID。
   */
  public long create(String name, String description) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              // 显式指定返回主键列 "id"
              var ps =
                      connection.prepareStatement(
                              "insert into knowledge_bases(name, description) values (?, ?)",
                              new String[] {"id"});
              ps.setString(1, name);
              ps.setString(2, description);
              return ps;
            },
            keyHolder);

    // 提取并校验生成的主键
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create knowledge base: No ID returned");
    }
    return key.longValue();
  }

  /**
   * 根据 ID 查询知识库。
   * 使用 Optional 容器处理结果可能不存在的情况。
   */
  public Optional<KnowledgeBase> findById(long id) {
    List<KnowledgeBase> rows =
            jdbcTemplate.query("select * from knowledge_bases where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 分页获取知识库列表。
   * 采用传统的 LIMIT/OFFSET 模式，按 ID 倒序排列以确保新创建的库排在前面。
   */
  public List<KnowledgeBase> list(int page, int pageSize) {
    // 偏移量计算：(页码 - 1) * 每页条数。
    // 使用 Math.max 确保 offset 不为负数。
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
            "select * from knowledge_bases order by id desc limit ? offset ?",
            mapper(),
            pageSize,
            offset);
  }

  /**
   * 统计全量知识库数量。
   * 用于支持前端分页组件计算总页数。
   */
  public long countAll() {
    Long total = jdbcTemplate.queryForObject("select count(*) from knowledge_bases", Long.class);
    return total == null ? 0 : total;
  }

  /**
   * 结果集映射器工厂方法。
   */
  private RowMapper<KnowledgeBase> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * JDBC ResultSet 到领域模型 KnowledgeBase 的静态映射逻辑。
   * 负责处理 SQL 类型到 Java 类型的转换细节。
   */
  private static KnowledgeBase map(ResultSet rs) throws SQLException {
    return new KnowledgeBase(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  /**
   * 工具方法：将数据库 Timestamp 安全转换为 Java 8 Instant。
   */
  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}