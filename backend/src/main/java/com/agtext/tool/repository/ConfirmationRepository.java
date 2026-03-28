package com.agtext.tool.repository;

import com.agtext.tool.domain.ConfirmationItem;
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
 * 确认单数据仓库
 * 负责 confirmations 表的读写，支持基于幂等键的查询
 */
@Repository
public class ConfirmationRepository {
    private final JdbcTemplate jdbcTemplate;

    public ConfirmationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据幂等键查询确认单
     * 用于在创建前检查是否已存在相同的请求，确保操作的幂等性
     */
    public Optional<ConfirmationItem> findByIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        List<ConfirmationItem> rows =
                jdbcTemplate.query("select * from confirmations where idempotency_key=?", mapper(), key);
        return rows.stream().findFirst();
    }

    /**
     * 创建新的确认单
     * 默认初始状态为 "pending"，请求者硬编码为 "agent"
     */
    public ConfirmationItem create(
            String idempotencyKey,
            String actionType,
            String refType,
            String refId,
            String summary,
            String payload) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    // 使用 PrepareStatement 插入数据并要求返回自增主键 "id"
                    var ps =
                            connection.prepareStatement(
                                    "insert into confirmations(requester, action, idempotency_key, payload_summary, status, actor, action_type, ref_type, ref_id, summary, payload) values (?,?,?,?,?,?,?,?,?,?,?)",
                                    new String[] {"id"});
                    ps.setString(1, "agent");
                    ps.setString(2, actionType);
                    ps.setString(3, idempotencyKey);
                    ps.setString(4, summary);
                    ps.setString(5, "pending"); // 初始状态：待处理
                    ps.setString(6, "agent");
                    ps.setString(7, actionType);
                    ps.setString(8, refType);
                    ps.setString(9, refId);
                    ps.setString(10, summary);
                    ps.setString(11, payload);
                    return ps;
                },
                keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create confirmation");
        }
        // 插入成功后立即查询完整对象返回，确保获取数据库生成的默认字段（如时间戳）
        return findById(key.longValue())
                .orElseThrow(() -> new IllegalStateException("Confirmation not found"));
    }

    /**
     * 按主键 ID 查询确认单
     */
    public Optional<ConfirmationItem> findById(long id) {
        List<ConfirmationItem> rows =
                jdbcTemplate.query("select * from confirmations where id=?", mapper(), id);
        return rows.stream().findFirst();
    }

    /**
     * 更新确认单状态
     * 通常用于从 "pending" 变更为 "approved" 或 "denied"
     */
    public void updateStatus(long id, String status) {
        jdbcTemplate.update("update confirmations set status=? where id=?", status, id);
    }

    private RowMapper<ConfirmationItem> mapper() {
        return (rs, rowNum) -> map(rs);
    }

    /**
     * 结果集映射逻辑
     * 包含对旧版本字段（action, payload_summary）与新版本字段（action_type, summary）的兼容处理
     */
    private static ConfirmationItem map(ResultSet rs) throws SQLException {
        // 兼容性逻辑：优先使用新字段 action_type，若为空则退回到 action
        String actionType = rs.getString("action_type");
        if (actionType == null || actionType.isBlank()) {
            actionType = rs.getString("action");
        }

        // 兼容性逻辑：优先使用新字段 summary，若为空则退回到 payload_summary
        String summary = rs.getString("summary");
        if (summary == null || summary.isBlank()) {
            summary = rs.getString("payload_summary");
        }

        return new ConfirmationItem(
                rs.getLong("id"),
                rs.getString("status"),
                actionType,
                rs.getString("ref_type"),
                rs.getString("ref_id"),
                summary,
                rs.getString("payload"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}