package com.zxx.springaitest.memory.custom;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IncrementalJdbcChatMemoryRepository implements ChatMemoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "spring_ai_chat_memory";

    public IncrementalJdbcChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> findConversationIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT conversation_id FROM " + TABLE_NAME, String.class);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 保持原有查询逻辑不变，但建议加上 ORDER BY timestamp ASC
        return jdbcTemplate.query(
                "SELECT content, type, timestamp, message_id FROM " + TABLE_NAME + " WHERE conversation_id = ? ORDER BY timestamp ASC",
                (rs, rowNum) -> {
                    String content = rs.getString("content");
                    String typeStr = rs.getString("type");
                    Instant timestamp = rs.getTimestamp("timestamp").toInstant();
                    String messageId = rs.getString("message_id");

                    MessageType type = MessageType.valueOf(typeStr.toUpperCase());
                    // 这里使用 MessageBuilder 或 switch-case 重建消息，视你的 Spring AI 版本而定
                    // 假设使用通用构建方式
                    return switch (type) {
                        case USER -> {
                            UserMessage userMessage = new UserMessage(content);
                            Map<String, Object> metadata = new HashMap<>(userMessage.getMetadata());
                            metadata.put("message_id", messageId);

                            yield userMessage.mutate()
                                    .metadata(metadata)
                                    .build();
                        }
                        case ASSISTANT -> {
                            AssistantMessage assistantMessage = new AssistantMessage(content);
                            Map<String, Object> metadata = new HashMap<>(assistantMessage.getMetadata());
                            metadata.put("message_id", messageId);

                            yield AssistantMessage.builder()
                                    .content(assistantMessage.getText())              // 保留原有文本内容
                                    .properties(metadata)                             // 注入包含新数据的 metadata
                                    .toolCalls(assistantMessage.getToolCalls())    // 保留原有的 toolCalls
                                    .media(assistantMessage.getMedia())            // 保留原有的媒体文件（如有）
                                    .build();
                        }
                        case SYSTEM -> new SystemMessage(content);
                        default -> throw new IllegalArgumentException("Unknown type: " + type);
                    };
                },
                conversationId
        );
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.notNull(conversationId, "conversationId must not be null");
        Assert.notNull(messages, "messages must not be null");

        if (messages.isEmpty()) return;

        // 批量插入，使用 INSERT IGNORE 防止重复
        String sql = "INSERT IGNORE INTO " + TABLE_NAME +
                " (conversation_id, message_id, content, type, timestamp) VALUES (?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = messages.stream().map(msg -> {
            // 核心：从 Metadata 中获取 ID，如果没有则生成一个新的
            String messageId = msg.getMetadata().getOrDefault("message_id", UUID.randomUUID().toString()).toString();

            return new Object[]{
                    conversationId,
                    messageId,
                    msg.getText(),
                    msg.getMessageType().name(),
                    Timestamp.from(Instant.now())
            };
        }).toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        System.out.println("deleteByConversationId");
        // 增量模式下通常不需要清空，除非用户主动要求重置会话
        // jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE conversation_id = ?", conversationId);
    }
}