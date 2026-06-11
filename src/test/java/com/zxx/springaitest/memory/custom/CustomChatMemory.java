package com.zxx.springaitest.memory.custom;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 自定义 ChatMemory 实现
 * 核心逻辑：在 add 时只提取增量消息存入 Repository，在 get 时交由 Repository 返回最新 N 条
 */
public class CustomChatMemory implements ChatMemory {

    private final ChatMemoryRepository chatMemoryRepository;
    private final int maxMessages;

    public CustomChatMemory(ChatMemoryRepository chatMemoryRepository, int maxMessages) {
        Assert.notNull(chatMemoryRepository, "ChatMemoryRepository cannot be null");
        Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
        this.chatMemoryRepository = chatMemoryRepository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        if (messages.isEmpty()) {
            return;
        }

        // 1. 获取当前数据库中该会话已有的消息数量
        List<Message> existingMessages = chatMemoryRepository.findByConversationId(conversationId);
        int existingCount = existingMessages.size();

        // 2. 计算增量：如果传入的列表长度大于数据库已有的长度，多出来的就是增量
        // 注意：Spring AI 的 MessageWindowChatMemory 传入的 messages 包含了历史+新消息
        // 如果传进来的消息数 <= 数据库已有数，说明没有新消息（可能是重复调用）
        if (messages.size() <= existingCount) {
            return;
        }

        // 3. 截取增量部分（从 existingCount 到末尾）
        List<Message> incrementalMessages = messages.subList(existingCount, messages.size());

        // 4. 将增量消息存入数据库
        // 这里我们直接调用 Repository 的 saveAll，因为传入的已经是纯增量数据，
        // 不需要 Repository 再去判断是否重复
        chatMemoryRepository.saveAll(conversationId, incrementalMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        // 从 Repository 获取数据
        // 注意：标准的 ChatMemoryRepository.findByConversationId 没有 maxMessages 参数
        // 我们需要在内存中截取最新的 maxMessages 条，或者自定义 Repository 支持 limit
        List<Message> allMessages = chatMemoryRepository.findByConversationId(conversationId);

        if (allMessages.size() <= maxMessages) {
            return allMessages;
        }
        // 返回最新的 maxMessages 条
        return allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
    }

    @Override
    public void clear(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        chatMemoryRepository.deleteByConversationId(conversationId);
    }
}