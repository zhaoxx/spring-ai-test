package com.zxx.springaitest;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestStructuredOut {

    private ChatClient chatClient;

    @BeforeEach
    public void init(@Autowired DashScopeChatModel chatModel) {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(ChatOptions.builder().temperature(0.1).build())
                .build();
    }

    public record Address (
        String name,
        String phone,
        String province,
        String city,
        String district,
        String detail
    ){}

    @Test
    public void testStructuredOut() {
        Address address = chatClient.prompt()
                .user("江西省永修县白槎镇双丰村下房组 赵XX 13019021982")
                .call()
                .entity(Address.class);
        System.out.println(address);
    }
}
