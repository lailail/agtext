package com.agtext.chat.repository;

import com.agtext.chat.domain.Message;
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
 * 消息持久化层。
 * 负责对话历史的结构化存储。在大模型应用中，此类操作通常在对话结束后同步执行，以确保上下文持久化。
 */
@Repository
public class MessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 持久化单条对话消息。
     * 采用显式的 PreparedStatement 处理，以防止 SQL 注入。
     * @param tokens 允许为空，因为某些流式输出或工具调用可能在初始阶段无法确定 Token 数。
     * @return 数据库生成的唯一消息 ID。
     */
    public long create(
            long conversationId,
            String role,
            String content,
            String provider,
            String modelName,
            Integer tokens) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    var ps =
                            connection.prepareStatement(
                                    "insert into messages(conversation_id, role, content, provider, model_name, tokens) values (?,?,?,?,?,?)",
                                    new String[] {"id"});
                    ps.setLong(1, conversationId);
                    ps.setString(2, role);
                    ps.setString(3, content);
                    ps.setString(4, provider);
                    ps.setString(5, modelName);
                    // 处理 Integer 类型的空值映射，确保数据库兼容性
                    if (tokens == null) {
                        ps.setObject(6, null);
                    } else {
                        ps.setInt(6, tokens);
                    }
                    return ps;
                },
                keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create message: No ID returned");
        }
        return key.longValue();
    }

    /**
     * 获取指定会话的全量消息历史。
     * 使用 'order by id asc' 确保消息严格按照时间线先后顺序排列，这是 LLM 正确理解上下文的基础。
     */
    public List<Message> listByConversationId(long conversationId) {
        return jdbcTemplate.query(
                "select * from messages where conversation_id=? order by id asc", mapper(), conversationId);
    }

    /**
     * 获取最近的 N 条消息。
     * 用于“滑动窗口”式的上下文管理，避免超出大模型的上下文窗口（Context Window）限制。
     */
    public List<Message> listRecentByConversationId(long conversationId, int limit) {
        return jdbcTemplate.query(
                "select * from messages where conversation_id=? order by id desc limit ?",
                mapper(),
                conversationId,
                limit);
    }

    private RowMapper<Message> mapper() {
        return (rs, rowNum) -> mapMessage(rs);
    }

    /**
     * 结果集映射。
     * 需注意 tokens 的读取需使用 getObject 以正确处理 SQL NULL。
     */
    private static Message mapMessage(ResultSet rs) throws SQLException {
        Integer tokens = (Integer) rs.getObject("tokens");
        return new Message(
                rs.getLong("id"),
                rs.getLong("conversation_id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("provider"),
                rs.getString("model_name"),
                tokens,
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}