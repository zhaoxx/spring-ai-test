package com.zxx.springaitest.memory;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest
public class TestJDBCMemory {

    @TestConfiguration
    static class Config {
        @Bean
        ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository){
            return MessageWindowChatMemory
                    .builder()
                    .maxMessages(10)
                    .chatMemoryRepository(chatMemoryRepository)
                    .build();
        }
    }

    private ChatClient chatClient;

    @BeforeEach
    public void init(@Autowired DashScopeChatModel chatModel,
                     @Autowired ChatMemory chatMemory) {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build())
                .defaultOptions(ChatOptions.builder().temperature(0.1).build())
                .build();
    }

    @Test
    public void testJDBCMemory(@Autowired ChatMemory chatMemory){
        String content1 = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,"1"))
                .user("我是赵一一")
                .call()
                .content();
        System.out.println(content1);
        System.out.println("/////////////////////////////////");

        String content2 = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,"1"))
                .user("我是谁？")
                .call()
                .content();
        System.out.println(content2);
    }

}
