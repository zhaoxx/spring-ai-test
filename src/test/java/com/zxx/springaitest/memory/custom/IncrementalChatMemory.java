package com.zxx.springaitest.memory.custom;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增量式 ChatMemory 实现
 * 核心逻辑：对比本地缓存与传入的新消息列表，仅提取出“未存储过”的新消息进行保存。
 */
public class IncrementalChatMemory implements ChatMemory {

    // 1. 委托给自定义的增量 Repository 进行物理存储
    private final IncrementalJdbcChatMemoryRepository repository;
    private int lastN = 10;

    public IncrementalChatMemory(IncrementalJdbcChatMemoryRepository repository, int lastN) {
        this.repository = repository;
        this.lastN = lastN;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 2. 【关键步骤】从数据库加载当前会话已有的消息
        // 注意：这里我们假设 Repository 实现了带 ID 的查询，或者至少能查出所有消息
        List<Message> existingMessages = this.repository.findByConversationId(conversationId);

        // 3. 构建一个“已存在消息ID”的集合，用于 O(1) 快速查找
        // 如果消息没有 ID（旧数据），则根据内容+类型生成临时指纹作为 Key
        List<String> existingKeys = existingMessages.stream()
                .map(this::extractMessageKey)
                .collect(Collectors.toList());

        // 4. 过滤出新消息
        List<Message> newMessagesToSave = new ArrayList<>();

        for (Message msg : messages) {
            String currentKey = extractMessageKey(msg);

            // 只有当这个 Key 不在数据库中时，才视为新消息
            if (!existingKeys.contains(currentKey)) {
                newMessagesToSave.add(msg);
            }
        }

        // 5. 仅保存新消息
        if (!newMessagesToSave.isEmpty()) {
            this.repository.saveAll(conversationId, newMessagesToSave);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        // 获取逻辑保持不变，直接查库
        List<Message> allMessages = this.repository.findByConversationId(conversationId);

        // 处理 lastN 逻辑（取最后 N 条）
        if (lastN > 0 && allMessages.size() > lastN) {
            return allMessages.subList(allMessages.size() - lastN, allMessages.size());
        }
        return allMessages;
    }

    @Override
    public void clear(String conversationId) {
        this.repository.deleteByConversationId(conversationId);
    }

    /**
     * 提取消息的唯一标识 Key
     * 优先级：UUID (由上层或模型生成) > 内容+类型指纹
     */
    private String extractMessageKey(Message message) {
        // 尝试从 Metadata 中获取我们在上层注入的 UUID
        Object idObj = message.getMetadata().get("message_id");
        if (idObj != null && StringUtils.hasText(idObj.toString())) {
            return idObj.toString();
        }

        // 兜底策略：如果没有 ID，使用 "类型:内容" 作为唯一键
        // 这解决了你之前遇到的 "User重复提问被误判为旧消息" 的问题吗？
        // 实际上，如果使用了 UUID 方案，这里几乎永远走不到。
        // 但为了兼容，保留此逻辑。
        return message.getMessageType().name() + ":" + message.getText();
    }


}