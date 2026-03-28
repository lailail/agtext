package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeDocument;
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
 * 知识文档持久层：负责管理知识库内原始文档的元数据及其处理状态。
 * 该类是 RAG 异步流水线（解析、切片、向量化）的状态同步核心。
 */
@Repository
public class KnowledgeDocumentRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeDocumentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建文档记录。
   * 默认初始状态为 'pending'，等待后台任务执行解析与索引逻辑。
   */
  public long create(long knowledgeBaseId, String sourceType, String sourceUri, String title) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              var ps =
                      connection.prepareStatement(
                              "insert into knowledge_documents(knowledge_base_id, source_type, source_uri, title, status) values (?,?,?,?,?)",
                              new String[] {"id"});
              ps.setLong(1, knowledgeBaseId);
              ps.setString(2, sourceType);
              ps.setString(3, sourceUri);
              ps.setString(4, title);
              ps.setString(5, "pending");
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create knowledge document");
    }
    return key.longValue();
  }

  /**
   * 根据 ID 精确查询文档详情。
   */
  public Optional<KnowledgeDocument> findById(long id) {
    List<KnowledgeDocument> rows =
            jdbcTemplate.query("select * from knowledge_documents where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 阶段性更新：解析完成后同步文档状态。
   * 核心逻辑：记录解析是否成功、内容哈希值（用于去重）以及关联的任务 ID。
   */
  public void updateAfterParse(
          long id,
          String status,
          String parseStatus,
          String indexStatus,
          String errorMessage,
          String contentHash,
          Long latestImportJobId) {
    jdbcTemplate.update(
            "update knowledge_documents set status=?, parse_status=?, index_status=?, error_message=?, content_hash=?, latest_import_job_id=? where id=?",
            status,
            parseStatus,
            indexStatus,
            errorMessage,
            contentHash,
            latestImportJobId,
            id);
  }

  /**
   * 索引就绪更新：将文档标记为 'ready' 可检索状态。
   * 逻辑点：同步更新 active_import_job_id，标识当前线上检索生效的向量版本。
   */
  public void updateIndexReady(long id, long jobId, String indexStatus) {
    jdbcTemplate.update(
            "update knowledge_documents set status='ready', index_status=?, active_import_job_id=?, latest_import_job_id=? where id=?",
            indexStatus,
            jobId,
            jobId,
            id);
  }

  /**
   * 活跃索引版本更新：主要用于重索引或版本切换场景。
   * 仅更新当前生效的任务 ID，不影响任务尝试记录（latest_import_job_id）。
   */
  public void updateActiveIndex(long id, long jobId, String indexStatus) {
    jdbcTemplate.update(
            "update knowledge_documents set status='ready', index_status=?, active_import_job_id=? where id=?",
            indexStatus,
            jobId,
            id);
  }

  /**
   * 按知识库 ID 分页查询文档列表。
   * 按 ID 倒序排列，优先展示最新上传的文档。
   */
  public List<KnowledgeDocument> listByBaseId(long knowledgeBaseId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
            "select * from knowledge_documents where knowledge_base_id=? order by id desc limit ? offset ?",
            mapper(),
            knowledgeBaseId,
            pageSize,
            offset);
  }

  /**
   * 统计指定知识库下的文档总数。
   */
  public long countByBaseId(long knowledgeBaseId) {
    Long total =
            jdbcTemplate.queryForObject(
                    "select count(*) from knowledge_documents where knowledge_base_id=?",
                    Long.class,
                    knowledgeBaseId);
    return total == null ? 0 : total;
  }

  private RowMapper<KnowledgeDocument> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集映射：处理复杂的 Long 型外键映射（使用 getObject 兼容可为空的字段）。
   */
  private static KnowledgeDocument map(ResultSet rs) throws SQLException {
    return new KnowledgeDocument(
            rs.getLong("id"),
            rs.getLong("knowledge_base_id"),
            rs.getString("source_type"),
            rs.getString("source_uri"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getString("parse_status"),
            rs.getString("index_status"),
            rs.getString("error_message"),
            rs.getString("content_hash"),
            // 处理可能为 null 的 Job ID 字段
            (Long) rs.getObject("latest_import_job_id"),
            (Long) rs.getObject("active_import_job_id"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}