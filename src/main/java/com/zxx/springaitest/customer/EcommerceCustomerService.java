package com.zxx.springaitest.customer;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.zxx.springaitest.config.MessageIdInjectionAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class EcommerceCustomerService {

    private final ChatClient chatClient;

    public EcommerceCustomerService(@Autowired DashScopeChatModel chatModel, @Autowired ChatMemory chatMemory, OrderTools orderTools) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);

        this.chatClient = builder
                // 1. 设置系统提示词，定义客服人设与行为规范
                .defaultSystem("""
                        您是电商平台的专属智能客服。请以友好、专业的语气回复。
                        在查询或取消订单前，必须先向用户确认订单号。
                        如果用户要求取消订单，请明确告知退款规则。
                        请始终使用中文回复。
                        """)
                // 2. 挂载多轮对话记忆（Memory）
                // 默认存储在内存中，生产环境可替换为 RedisChatMemory
                .defaultAdvisors(new MessageIdInjectionAdvisor(), PromptChatMemoryAdvisor.builder(chatMemory).build())
                // 3. 注册订单管理工具（Function Calling）
                .defaultTools(orderTools)
                .build();
    }

    /**
     * 处理用户对话
     * @param userMessage 用户输入
     * @param conversationId 会话ID（用于隔离不同用户的记忆）
     */
    public Flux<String> chat(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                // 传入会话ID，让Memory能够区分并召回当前用户的上下文
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}