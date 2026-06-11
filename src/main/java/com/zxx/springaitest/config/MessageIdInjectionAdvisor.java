package com.zxx.springaitest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
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

//    @Override
//    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
//        // 如果需要对 AI 的响应（AssistantMessage）也注入 ID 并保存，可以在这里处理
//        // 通常 AI 的响应 ID 由底层模型或数据库生成，这里按需扩展
//        return chatClientResponse;
//    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 拦截 AI 的响应，为 ASSISTANT 消息注入 message_id
        if (chatClientResponse != null && chatClientResponse.chatResponse() != null) {
            AssistantMessage originalAssistantMsg = chatClientResponse.chatResponse().getResult().getOutput();

            Map<String, Object> metadata = new HashMap<>(originalAssistantMsg.getMetadata());
            if (!metadata.containsKey("message_id")) {
//                String uuid = UUID.randomUUID().toString();
//                metadata.put("message_id", uuid);

//                // 使用 mutate() 重建 AssistantMessage
//                AssistantMessage updatedAssistantMsg = originalAssistantMsg.mutate()
//                        .metadata(metadata)
//                        .build();
//
//                log.info("成功注入 ASSISTANT message_id: {}", uuid);

                // 注意：由于 ChatResponse 和 Result 的结构较深，
                // 这里假设你使用的 Spring AI 版本支持对响应进行 mutate。
                // 如果不支持直接 mutate 响应，你可能需要在你的 CustomJDBCMemory 中
                // 针对 ASSISTANT 消息做兜底生成 ID 的处理。

                // 返回修改后的响应（具体 API 视你的 Spring AI 版本而定）
                // 这里给出一个通用的处理思路：
                /*
                ChatResponse newChatResponse = chatClientResponse.chatResponse().mutate()
                        .result(new Result(updatedAssistantMsg))
                        .build();
                return chatClientResponse.mutate()
                        .chatResponse(newChatResponse)
                        .build();
                */
            }
        }

        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        // 设置为最高优先级，确保在任何其他 Advisor（如 Memory、RAG）之前执行
        // 这样能保证进入存储层或大模型的消息都已经带上了 ID
        return Ordered.HIGHEST_PRECEDENCE;
    }
}