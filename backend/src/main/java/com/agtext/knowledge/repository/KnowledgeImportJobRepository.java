package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeImportJob;
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
 * 知识导入任务持久层：负责 knowledge_import_jobs 表的操作。
 * 该类是 RAG 异步流水线的核心状态机实现，记录任务的排队、运行、成功、失败及取消状态。
 */
@Repository
public class KnowledgeImportJobRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeImportJobRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建并初始化导入任务。
   * 逻辑：任务初始状态固定为 'queued'（排队中），进度为 0，started_at 为空。
   * @param knowledgeBaseId 所属知识库
   * @param documentId 关联文档（可选，部分任务可能先创建 Job 再生成 Document）
   */
  public long create(long knowledgeBaseId, Long documentId) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              var ps =
                      connection.prepareStatement(
                              "insert into knowledge_import_jobs(knowledge_base_id, document_id, status, stage, progress, started_at) values (?,?,?,?,?,?)",
                              new String[] {"id"});
              ps.setLong(1, knowledgeBaseId);
              if (documentId == null) {
                ps.setObject(2, null);
              } else {
                ps.setLong(2, documentId);
              }
              ps.setString(3, "queued");
              ps.setString(4, "queued");
              ps.setInt(5, 0);
              ps.setObject(6, null);
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create import job");
    }
    return key.longValue();
  }

  /**
   * 按 ID 查询任务详情。
   */
  public Optional<KnowledgeImportJob> findById(long id) {
    List<KnowledgeImportJob> rows =
            jdbcTemplate.query("select * from knowledge_import_jobs where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 多维度分页查询任务列表。
   * 逻辑：根据传入参数动态构建 SQL 过滤条件，支持按知识库、文档或状态筛选。
   */
  public List<KnowledgeImportJob> list(
          Long knowledgeBaseId, Long documentId, String status, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from knowledge_import_jobs where 1=1");
    List<Object> args = new ArrayList<>();

    if (knowledgeBaseId != null) {
      sql.append(" and knowledge_base_id=?");
      args.add(knowledgeBaseId);
    }
    if (documentId != null) {
      sql.append(" and document_id=?");
      args.add(documentId);
    }
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
   * 统计符合条件的任务总数。
   * 逻辑：与 list 方法的过滤条件保持完全一致，用于分页计算。
   */
  public long count(Long knowledgeBaseId, Long documentId, String status) {
    StringBuilder sql = new StringBuilder("select count(*) from knowledge_import_jobs where 1=1");
    List<Object> args = new ArrayList<>();
    if (knowledgeBaseId != null) {
      sql.append(" and knowledge_base_id=?");
      args.add(knowledgeBaseId);
    }
    if (documentId != null) {
      sql.append(" and document_id=?");
      args.add(documentId);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  /**
   * 更新任务为运行状态。
   * 逻辑：使用 coalesce 确保 started_at 仅在第一次启动时记录，后续重试或更新阶段时不覆盖初次启动时间。
   */
  public void markRunning(long jobId, String stage, int progress) {
    jdbcTemplate.update(
            "update knowledge_import_jobs set status='running', stage=?, progress=?, started_at=coalesce(started_at, current_timestamp) where id=?",
            stage,
            progress,
            jobId);
  }

  /**
   * 标记任务执行成功。
   * 逻辑：将进度强制设为 100，并记录任务完成时间。
   */
  public void markSucceeded(long jobId) {
    jdbcTemplate.update(
            "update knowledge_import_jobs set status='succeeded', stage='done', progress=100, finished_at=current_timestamp where id=?",
            jobId);
  }

  /**
   * 标记任务执行失败。
   * 逻辑：记录失败时的业务阶段及具体错误详情，并记录终止时间。
   */
  public void markFailed(long jobId, String stage, String errorMessage) {
    jdbcTemplate.update(
            "update knowledge_import_jobs set status='failed', stage=?, error_message=?, finished_at=current_timestamp where id=?",
            stage,
            errorMessage,
            jobId);
  }

  /**
   * 标记任务为取消状态。
   * 逻辑：记录取消时间及任务终结时间。
   */
  public void cancel(long jobId) {
    jdbcTemplate.update(
            "update knowledge_import_jobs set status='cancelled', cancelled_at=current_timestamp, finished_at=current_timestamp where id=?",
            jobId);
  }

  private RowMapper<KnowledgeImportJob> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集到领域模型的映射逻辑。
   * 包含对可为空的 Long 类型字段（document_id）和 Integer 类型字段（progress）的处理。
   */
  private static KnowledgeImportJob map(ResultSet rs) throws SQLException {
    return new KnowledgeImportJob(
            rs.getLong("id"),
            rs.getLong("knowledge_base_id"),
            (Long) rs.getObject("document_id"),
            rs.getString("status"),
            toInstant(rs.getTimestamp("started_at")),
            toInstant(rs.getTimestamp("finished_at")),
            rs.getString("stage"),
            (Integer) rs.getObject("progress"),
            rs.getString("error_message"),
            toInstant(rs.getTimestamp("cancelled_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}