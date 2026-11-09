package com.zxx.springaitest.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientInitConfiguration {

    // 智能规划
    @Bean
    public ChatClient planningChatClient(DashScopeChatModel dashScopeChatModel,
                                         DashScopeChatProperties options,
                                         ChatMemory chatMemory) {
        DashScopeChatOptions dashScopeChatOptions = DashScopeChatOptions.fromOptions(options.getOptions());
        dashScopeChatOptions.setTemperature(0.1);

        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        # 订单助手任务分拆规则
                        ## 1. 要求
                        ### 1.1 根据用户内容识别任务
                        
                        ## 2. 任务
                        ### 2.1 JobType:取消(CANCEL) 要求用户提供姓名和订单号，或者从对话记忆中查找
                        ### 2.2 JobType:查询(QUERY) 要求用户提供订单号，或者从对话记忆中查找
                        ### 2.3 JobType:其他(OTHER)
                        """)
                .defaultOptions(dashScopeChatOptions)
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
                .build();
    }

    // 智能客服
    @Bean
    public ChatClient botChatClient(DashScopeChatModel dashScopeChatModel,
                                    DashScopeChatProperties options,
                                    ChatMemory chatMemory) {
        DashScopeChatOptions dashScopeChatOptions = DashScopeChatOptions.fromOptions(options.getOptions());
        dashScopeChatOptions.setTemperature(1.2);

        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        你是**智能客服代理，请以友好、热情的态度服务用户。
                        """
                )
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
                .defaultOptions(dashScopeChatOptions)
                .build();
    }
}
