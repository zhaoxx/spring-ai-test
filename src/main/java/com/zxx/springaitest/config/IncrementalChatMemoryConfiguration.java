package com.zxx.springaitest.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class IncrementalChatMemoryConfiguration {

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
