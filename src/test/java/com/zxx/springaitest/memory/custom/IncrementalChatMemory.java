package com.zxx.springaitest.memory.custom;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增量式聊天记忆管理器
 * 负责维护本地状态，并只将“新增”的消息同步到数据库
 */
public class IncrementalChatMemory implements ChatMemory {

    private final ChatMemoryRepository repository;
    private final int maxMessages;

    // 本地缓存：用于快速比对哪些消息是新来的
    // Key: ConversationID, Value: 消息列表
    private final Map<String, List<Message>> localCache = new ConcurrentHashMap<>();

    public IncrementalChatMemory(ChatMemoryRepository repository, int maxMessages) {
        this.repository = repository;
        this.maxMessages = maxMessages;
    }

    @Override
    public List<Message> get(String conversationId) {
        // 优先从本地缓存获取，如果没有则查库
        List<Message> cached = localCache.get(conversationId);
        if (cached != null) {
            return cached;
        }

        List<Message> dbMessages = repository.findByConversationId(conversationId);
        localCache.put(conversationId, dbMessages);
        return dbMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        // 1. 获取当前已有的消息列表（用于计算差集）
        List<Message> currentMessages = get(conversationId);
        List<Message> allMessages = new ArrayList<>(currentMessages);

        // 2. 找出真正的新增消息（简单的引用比对或内容比对）
        List<Message> newMessages = new ArrayList<>();
        for (Message msg : messages) {
            // 这里简单判断是否已包含，实际生产环境建议根据 Message ID 或 内容Hash 判断
            boolean exists = currentMessages.stream()
                    .anyMatch(m -> m.getText().equals(msg.getText()) && m.getMessageType().equals(msg.getMessageType()));

            if (!exists) {
                newMessages.add(msg);
                allMessages.add(msg);
            }
        }

        // 3. 只有当有新消息时，才写入数据库
        if (!newMessages.isEmpty()) {
            // 写入数据库（Repository 内部做最后的防重兜底）
            repository.saveAll(conversationId, newMessages);
        }

        // 4. 更新本地缓存（应用窗口大小限制，防止本地内存溢出）
        if (allMessages.size() > maxMessages) {
            allMessages = allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
        }
        localCache.put(conversationId, allMessages);
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
        localCache.remove(conversationId);
    }
}