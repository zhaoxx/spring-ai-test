package com.zxx.springaitest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MessageIdInjectionAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 1. 获取原始的 Prompt
        Prompt originalPrompt = chatClientRequest.prompt();
        List<Message> originalMessages = originalPrompt.getInstructions();

        // 2. 遍历消息列表，为没有 message_id 的 USER 消息注入 UUID
        List<Message> updatedMessages = new ArrayList<>();
        boolean hasModified = false;

        for (Message message : originalMessages) {
            // 仅处理用户消息
            if (message instanceof UserMessage userMessage) {
                Map<String, Object> metadata = new HashMap<>(userMessage.getMetadata());

                // 检查是否已经存在 message_id
                if (!metadata.containsKey("message_id")) {
                    String uuid = UUID.randomUUID().toString();
                    metadata.put("message_id", uuid);

                    // 利用 Spring AI 的 mutate() 方法重建消息，注入新的 metadata
                    UserMessage updatedUserMessage = userMessage.mutate()
                            .metadata(metadata)
                            .build();

                    updatedMessages.add(updatedUserMessage);
                    hasModified = true;
                    log.info("成功注入 message_id: {} -> {}", uuid, userMessage.getText());
                } else {
                    updatedMessages.add(userMessage);
                }
            } else {
                // 非用户消息（如 SystemMessage）原样保留
                updatedMessages.add(message);
            }
        }

        // 3. 如果消息被修改过，使用 mutate() 重建 Prompt 和 Request
        if (hasModified) {
            Prompt newPrompt = originalPrompt.mutate()
                    .messages(updatedMessages)
                    .build();

            return chatClientRequest.mutate()
                    .prompt(newPrompt)
                    .build();
        }

        // 如果没有修改，直接返回原请求
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 拦截 AI 的响应，为 ASSISTANT 消息注入 message_id
        if (chatClientResponse != null && chatClientResponse.chatResponse() != null) {
            AssistantMessage originalAssistantMsg = chatClientResponse.chatResponse().getResult().getOutput();

            Map<String, Object> metadata = new HashMap<>(originalAssistantMsg.getMetadata());
            if (!metadata.containsKey("message_id")) {
                String uuid = UUID.randomUUID().toString();
                metadata.put("message_id", uuid);

                // 3. 重新构建 AssistantMessage
                AssistantMessage newAssistantMsg = AssistantMessage.builder()
                        .content(originalAssistantMsg.getText())              // 保留原有文本内容
                        .properties(metadata)                             // 注入包含新数据的 metadata
                        .toolCalls(originalAssistantMsg.getToolCalls())    // 保留原有的 toolCalls
                        .media(originalAssistantMsg.getMedia())            // 保留原有的媒体文件（如有）
                        .build();

                log.info("成功注入 ASSISTANT message_id: {}", uuid);

                // 4. 重新构建 ChatResponse 和 ChatClientResponse 并返回
                Generation newGeneration = new Generation(newAssistantMsg);
                ChatResponse newChatResponse = new ChatResponse(List.of(newGeneration));

                return chatClientResponse.mutate()
                        .chatResponse(newChatResponse)
                        .build();
            }
        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        // 设置为最高优先级，确保在 PromptChatMemoryAdvisor (order=-100) 之前执行
        // 这样能保证进入 Memory 存储的消息都已经带上了 message_id
        return 0;
    }
}