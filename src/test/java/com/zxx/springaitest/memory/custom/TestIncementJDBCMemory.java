package com.zxx.springaitest.memory.custom;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql(scripts = "/sql/schema-mysql.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class TestIncementJDBCMemory {

    @TestConfiguration
    static class Config {

        // 1. 注入原生的 JdbcChatMemoryRepository（或者自定义的纯 CRUD Repository）
        @Bean
        public ChatMemory chatMemory(JdbcTemplate jdbcTemplate) {
            int messageSize = 10;
            // 1. 创建自定义的增量 Repository
            IncrementalJdbcChatMemoryRepository repository = new IncrementalJdbcChatMemoryRepository(jdbcTemplate);

            // 2. 创建自定义的增量 Memory，设置最大保留 50 条上下文
            return new IncrementalChatMemory(repository, messageSize);
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
    public void testIncrementJDBCMemory(@Autowired ChatMemory chatMemory){
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
