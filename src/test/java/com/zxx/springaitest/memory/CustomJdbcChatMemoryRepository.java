package com.zxx.springaitest.memory;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 自定义JDBC记忆存储库
 * 1. 保存数据时：不做数量限制，全量存储
 * 2. 获取数据时：只取最新的 maxMessages 条
 */
public class CustomJdbcChatMemoryRepository implements ChatMemoryRepository {

    private final JdbcChatMemoryRepository delegate;
    private final JdbcTemplate jdbcTemplate;

    public CustomJdbcChatMemoryRepository(JdbcChatMemoryRepository delegate, JdbcTemplate jdbcTemplate) {
        this.delegate = delegate;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 重写保存逻辑：只插入，不删除旧数据
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 核心：直接调用底层的批量插入逻辑，绕过它自带的滑动窗口删除机制
        // 注意：这里需要根据你使用的 Spring AI 版本，调用对应的底层插入方法
        // 如果底层没有暴露纯插入方法，我们可以直接用 JdbcTemplate 执行插入 SQL
        String sql = "INSERT INTO SPRING_AI_CHAT_MEMORY (conversation_id, content, type, timestamp) VALUES (?, ?, ?, NOW())";

        jdbcTemplate.batchUpdate(sql, messages, messages.size(),
                (ps, message) -> {
                    ps.setString(1, conversationId);
                    ps.setString(2, message.getText());
                    ps.setString(3, message.getMessageType().name());
                });
    }

    /**
     * 获取数据时：委托给原生 Repository 处理（它内部会处理 maxMessages 的截取）
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    /**
     * 获取所有的会话ID：委托给原生 Repository 处理
     */
    @Override
    public List<String> findConversationIds() {
        return delegate.findConversationIds();
    }

    /**
     * 删除指定会话的所有记录
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }
}