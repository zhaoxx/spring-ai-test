package com.zxx.springaitest.memory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestMemory {

    private ChatClient chatClient;

    @BeforeEach
    public void init(@Autowired DashScopeChatModel chatModel,
                     @Autowired ChatMemory chatMemory) {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(ChatOptions.builder().temperature(0.1).build())
                .build();
    }

    @Test
    public void testNoMemory(){
        String chatHis = "我是赵一一";
        String content = chatClient.prompt()
                .user(chatHis)
                .call()
                .content();
        System.out.println(content);
        content += chatHis;
        System.out.println("/////////////////////////////////");

        chatHis = "我是谁？";
        content = chatClient.prompt()
                .user(chatHis)
                .call()
                .content();

        System.out.println(content);
    }


    @Test
    public void testMemory(){
        String chatHis = "我是赵一一";
        String content = chatClient.prompt()
                .user(chatHis)
                .call()
                .content();
        chatHis += content;
        System.out.println(chatHis);
        System.out.println("/////////////////////////////////");

        chatHis += "我是谁？";
        content = chatClient.prompt()
                .user(chatHis)
                .call()
                .content();

        chatHis += content;
        System.out.println(chatHis);
    }
}
