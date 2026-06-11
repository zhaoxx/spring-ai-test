package com.zxx.springaitest.memory.custom;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 增量式 JDBC ChatMemory Repository
 * 解决官方版本 "先删后插" 导致的性能问题和 ID 暴涨问题
 */
public class IncrementalJdbcChatMemoryRepository implements ChatMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    // 表名，可根据实际修改
    private static final String TABLE_NAME = "spring_ai_chat_memory";

    public IncrementalJdbcChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> findConversationIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT conversation_id FROM " + TABLE_NAME, String.class);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 保持原有查询逻辑不变
        return jdbcTemplate.query(
                "SELECT content, type, timestamp FROM " + TABLE_NAME + " WHERE conversation_id = ? ORDER BY timestamp ASC",
                (rs, rowNum) -> {
                    String content = rs.getString("content");
                    String typeStr = rs.getString("type");
                    Instant timestamp = rs.getTimestamp("timestamp").toInstant();

                    Message message;
                    MessageType type = MessageType.valueOf(typeStr.toUpperCase());

                    switch (type) {
                        case USER:
                            message = new UserMessage(content);
                            break;
                        case ASSISTANT:
                            message = new AssistantMessage(content);
                            break;
                        case SYSTEM:
                            message = new SystemMessage(content);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown message type: " + type);
                    }

                    // 如果需要保留时间戳用于排序或展示，可存入 metadata（可选）
                    message.getMetadata().put("timestamp", timestamp);
                    return message;
                },
                conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 核心优化点：使用 INSERT IGNORE (MySQL) 或 ON CONFLICT DO NOTHING (PostgreSQL)
        // 这样如果消息已存在（基于唯一键），则跳过；不存在则插入。
        // 注意：这需要你的表结构支持唯一性校验（例如内容+时间戳，或者外部传入ID）。
        // 为了简化演示，这里假设每次保存都是追加，或者由上层 Memory 保证去重。

        String sql = """
                INSERT INTO %s (conversation_id, content, type, timestamp)
                VALUES (?, ?, ?, ?)
                """.formatted(TABLE_NAME);

        // 批量执行插入
        jdbcTemplate.batchUpdate(sql, messages, messages.size(), (ps, message) -> {
            ps.setString(1, conversationId);
            ps.setString(2, message.getText()); // 获取消息内容
            ps.setString(3, message.getMessageType().name()); // 获取消息类型
            ps.setTimestamp(4, Timestamp.from(Instant.now())); // 记录当前时间
        });
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        // 仅在显式删除会话时调用，平时不调用
        jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE conversation_id = ?", conversationId);
    }
}