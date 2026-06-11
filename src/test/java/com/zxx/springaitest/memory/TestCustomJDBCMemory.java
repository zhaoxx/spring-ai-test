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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql(scripts = "/sql/schema-mysql.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class TestCustomJDBCMemory {

    @TestConfiguration
    static class Config {
        // 1. 注入自定义的 Repository
        @Bean
        CustomJdbcChatMemoryRepository customJdbcChatMemoryRepository(
                JdbcChatMemoryRepository jdbcChatMemoryRepository, // 注入原生实例
                JdbcTemplate jdbcTemplate) {
            return new CustomJdbcChatMemoryRepository(jdbcChatMemoryRepository, jdbcTemplate);
        }

        // 2. 在 ChatMemory Bean 中使用自定义的 Repository
        @Bean
        ChatMemory chatMemory(CustomJdbcChatMemoryRepository customJdbcChatMemoryRepository){
            return MessageWindowChatMemory
                    .builder()
                    .maxMessages(10) // 这个值依然生效，用于控制查询时返回的条数
                    .chatMemoryRepository(customJdbcChatMemoryRepository)
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
    public void testCustomerJDBCMemory(@Autowired ChatMemory chatMemory){
        String content1 = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,"1"))
                .user("你好")
                .call()
                .content();
        System.out.println(content1);
        System.out.println("/////////////////////////////////");

        String content2 = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,"1"))
                .user("你好")
                .call()
                .content();
        System.out.println(content2);
    }

}
