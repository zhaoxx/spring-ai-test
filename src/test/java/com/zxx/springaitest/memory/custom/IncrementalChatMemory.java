package com.zxx.springaitest.memory.custom;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 增量式 ChatMemory 实现
 * 核心逻辑：对比本地缓存与传入的新消息列表，仅提取出"未存储过"的新消息进行保存。
 * 在保存前为没有 message_id 的消息自动注入 UUID。
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
        List<Message> existingMessages = this.repository.findByConversationId(conversationId);

        // 3. 构建一个"已存在消息ID"的集合，用于 O(1) 快速查找
        List<String> existingKeys = existingMessages.stream()
                .map(this::extractMessageKey)
                .collect(Collectors.toList());

        // 4. 过滤出新消息，并在保存前注入 message_id
        List<Message> newMessagesToSave = new ArrayList<>();

        for (Message msg : messages) {
            String currentKey = extractMessageKey(msg);

            // 只有当这个 Key 不在数据库中时，才视为新消息
            if (!existingKeys.contains(currentKey)) {
                // 【关键】在保存前为消息注入 message_id
                Message msgWithId = ensureMessageId(msg);
                newMessagesToSave.add(msgWithId);
            }
        }

        // 5. 仅保存新消息
        if (!newMessagesToSave.isEmpty()) {
            this.repository.saveAll(conversationId, newMessagesToSave);
        }
    }

    /**
     * 确保消息有 message_id，如果没有则注入 UUID
     */
    private Message ensureMessageId(Message message) {
        Map<String, Object> metadata = new HashMap<>(message.getMetadata());

        // 如果已经有 message_id，直接返回原消息
        if (metadata.containsKey("message_id") && StringUtils.hasText(metadata.get("message_id").toString())) {
            return message;
        }

        // 注入新的 message_id
        String uuid = UUID.randomUUID().toString();
        metadata.put("message_id", uuid);

        // 根据消息类型重建消息
        if (message instanceof UserMessage userMessage) {
            UserMessage newMsg = userMessage.mutate()
                    .metadata(metadata)
                    .build();
            System.out.println("[IncrementalChatMemory] USER message_id: " + uuid);
            return newMsg;
        } else if (message instanceof AssistantMessage assistantMessage) {
            AssistantMessage newMsg = AssistantMessage.builder()
                    .content(assistantMessage.getText())
                    .properties(metadata)
                    .toolCalls(assistantMessage.getToolCalls())
                    .media(assistantMessage.getMedia())
                    .build();
            System.out.println("[IncrementalChatMemory] ASSISTANT message_id: " + uuid);
            return newMsg;
        }

        // 其他类型消息原样返回
        return message;
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
        return message.getMessageType().name() + ":" + message.getText();
    }

}
