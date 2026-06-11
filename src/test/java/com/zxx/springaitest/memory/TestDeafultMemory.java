package com.zxx.springaitest.memory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestDeafultMemory {

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
    public void testMemory(){
        String content1 = chatClient.prompt()
                .user("我是赵一一")
                .call()
                .content();
        System.out.println(content1);
        System.out.println("/////////////////////////////////");

        String content2 = chatClient.prompt()
                .user("我是谁？")
                .call()
                .content();

        System.out.println(content2);
    }

//    @Test
//    public void testMoreUserMemory(@Autowired ChatMemory chatMemory){
//        String content1 = chatClient.prompt()
//                .advisors(PromptChatMemoryAdvisor.builder(chatMemory).conversationId("1").build())
//                .user("我是赵一一")
//                .call()
//                .content();
//        System.out.println(content1);
//        System.out.println("/////////////////////////////////");
//
//        String content2 = chatClient.prompt()
//                .advisors(PromptChatMemoryAdvisor.builder(chatMemory).conversationId("2").build())
//                .user("我是谁？")
//                .call()
//                .content();
//
//        System.out.println(content2);
//    }

    @Test
    public void testMoreUserMemory2(@Autowired ChatMemory chatMemory){
        String content1 = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,"1"))
                .user("我是赵一一")
                .call()
                .content();
        System.out.println(content1);
        System.out.println("/////////////////////////////////");

        String content2 = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,"2"))
                .user("我是谁？")
                .call()
                .content();

        System.out.println(content2);
    }

}
