package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeChunk;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 知识分片持久层：负责管理文档切分后的文本片段（Chunks）。
 * 核心逻辑：
 * 1. 严格保证分片在文档内的物理顺序（chunk_index）。
 * 2. 支持基于导入任务（Import Job）的版本隔离查询。
 */
@Repository
public class KnowledgeChunkRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeChunkRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建分片记录。
   * @param chunkIndex 分片在原文档中的序号，检索后用于还原上下文顺序。
   * @param chunkHash 内容哈希，用于检测内容重复及实现增量索引优化。
   */
  public long create(
          long documentId, long importJobId, int chunkIndex, String content, String chunkHash) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              var ps =
                      connection.prepareStatement(
                              "insert into knowledge_chunks(knowledge_document_id, import_job_id, chunk_index, content, chunk_hash) values (?,?,?,?,?)",
                              new String[] {"id"});
              ps.setLong(1, documentId);
              ps.setLong(2, importJobId);
              ps.setInt(3, chunkIndex);
              ps.setString(4, content);
              ps.setString(5, chunkHash);
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create knowledge chunk");
    }
    return key.longValue();
  }

  /**
   * 获取指定文档的所有分片。
   * 显式指定 chunk_index asc 排序，确保文本逻辑连续。
   */
  public List<KnowledgeChunk> listAllByDocumentId(long documentId) {
    return jdbcTemplate.query(
            "select * from knowledge_chunks where knowledge_document_id=? order by chunk_index asc",
            mapper(),
            documentId);
  }

  /**
   * 获取特定文档在特定导入任务下的所有分片。
   * 用于预览或同步当前正在处理的任务版本数据。
   */
  public List<KnowledgeChunk> listAllByDocumentIdAndImportJobId(long documentId, long importJobId) {
    return jdbcTemplate.query(
            "select * from knowledge_chunks where knowledge_document_id=? and import_job_id=? order by chunk_index asc",
            mapper(),
            documentId,
            importJobId);
  }

  /**
   * 分页获取文档分片列表。
   */
  public List<KnowledgeChunk> listByDocumentId(long documentId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
            "select * from knowledge_chunks where knowledge_document_id=? order by chunk_index asc limit ? offset ?",
            mapper(),
            documentId,
            pageSize,
            offset);
  }

  /**
   * 分页获取特定导入任务下的文档分片。
   * 在文档重索引（Re-indexing）期间，此方法可确保只查出新版本的分片。
   */
  public List<KnowledgeChunk> listByDocumentIdAndImportJobId(
          long documentId, long importJobId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
            "select * from knowledge_chunks where knowledge_document_id=? and import_job_id=? order by chunk_index asc limit ? offset ?",
            mapper(),
            documentId,
            importJobId,
            pageSize,
            offset);
  }

  /**
   * 统计文档分片总数。
   */
  public long countByDocumentId(long documentId) {
    Long total =
            jdbcTemplate.queryForObject(
                    "select count(*) from knowledge_chunks where knowledge_document_id=?",
                    Long.class,
                    documentId);
    return total == null ? 0 : total;
  }

  /**
   * 统计特定导入任务下的分片总数。
   */
  public long countByDocumentIdAndImportJobId(long documentId, long importJobId) {
    Long total =
            jdbcTemplate.queryForObject(
                    "select count(*) from knowledge_chunks where knowledge_document_id=? and import_job_id=?",
                    Long.class,
                    documentId,
                    importJobId);
    return total == null ? 0 : total;
  }

  private RowMapper<KnowledgeChunk> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集映射逻辑。
   * 注意：import_job_id 可能为 null，需使用 getObject(..., Long.class) 安全转换。
   */
  private static KnowledgeChunk map(ResultSet rs) throws SQLException {
    return new KnowledgeChunk(
            rs.getLong("id"),
            rs.getLong("knowledge_document_id"),
            (Long) rs.getObject("import_job_id"),
            rs.getInt("chunk_index"),
            rs.getString("content"),
            rs.getString("chunk_hash"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}