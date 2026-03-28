package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.ParseReport;
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
 * 解析报告持久层：负责 parse_reports 表的操作。
 * 该类存储了 RAG 流程中“文档解析”阶段的量化指标，用于审计解析质量、统计字符消耗以及排查解析异常。
 */
@Repository
public class ParseReportRepository {
    private final JdbcTemplate jdbcTemplate;

    public ParseReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建解析报告记录。
     * * @param documentId 关联的知识文档 ID
     * @param jobId 触发此次解析的异步任务 ID
     * @param summary 解析结果摘要（如：成功、部分内容无法提取）
     * @param extractedChars 提取的有效字符总数，用于计算 Embedding 成本和索引密度
     * @param pageCount 文档页数（针对 PDF/Docx 等格式）
     * @param chunkCount 最终切分生成的片段数量
     * @param parserName 所使用的解析组件标识（如：tika, nougat, unstructured）
     * @param failedAt 记录解析中断的具体逻辑位置（如有）
     * @param samplePreview 提取文本的内容采样，用于前端快速预览核对
     */
    public long create(
            long documentId,
            Long jobId,
            String summary,
            Long extractedChars,
            Integer pageCount,
            Integer chunkCount,
            String parserName,
            String failedAt,
            String samplePreview) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    var ps =
                            connection.prepareStatement(
                                    "insert into parse_reports(knowledge_document_id, job_id, summary, extracted_chars, page_count, chunk_count, parser_name, failed_at, sample_preview) values (?,?,?,?,?,?,?,?,?)",
                                    new String[] {"id"});
                    ps.setLong(1, documentId);
                    // 处理可为空的 Long 字段
                    if (jobId == null) {
                        ps.setObject(2, null);
                    } else {
                        ps.setLong(2, jobId);
                    }
                    ps.setString(3, summary);
                    // 使用 setObject 处理可为空的包装类型（Long, Integer）
                    ps.setObject(4, extractedChars);
                    ps.setObject(5, pageCount);
                    ps.setObject(6, chunkCount);
                    ps.setString(7, parserName);
                    ps.setString(8, failedAt);
                    ps.setString(9, samplePreview);
                    return ps;
                },
                keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create parse report");
        }
        return key.longValue();
    }

    /**
     * 根据任务 ID 获取最近的一份解析报告。
     * 逻辑：当同一个任务由于重试产生多次报告时，该方法确保获取当前最新的审计结果。
     */
    public Optional<ParseReport> findLatestByJobId(long jobId) {
        List<ParseReport> rows =
                jdbcTemplate.query(
                        "select * from parse_reports where job_id=? order by id desc limit 1", mapper(), jobId);
        return rows.stream().findFirst();
    }

    private RowMapper<ParseReport> mapper() {
        return (rs, rowNum) -> map(rs);
    }

    /**
     * 结果集到 ParseReport 领域对象的映射。
     * 针对数据库中可能为 NULL 的数值字段（job_id, extracted_chars 等），采用 (T) rs.getObject() 方式进行安全转型。
     */
    private static ParseReport map(ResultSet rs) throws SQLException {
        return new ParseReport(
                rs.getLong("id"),
                rs.getLong("knowledge_document_id"),
                (Long) rs.getObject("job_id"),
                rs.getString("summary"),
                (Long) rs.getObject("extracted_chars"),
                (Integer) rs.getObject("page_count"),
                (Integer) rs.getObject("chunk_count"),
                rs.getString("parser_name"),
                rs.getString("failed_at"),
                rs.getString("sample_preview"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}