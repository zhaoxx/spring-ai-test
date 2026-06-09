package com.zxx.springaitest;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

@SpringBootTest
class SpringAiTestApplicationTests {

    @Test
    void testCall(@Autowired DeepSeekChatModel chatModel) {
        String response = chatModel.call("你好");
        System.out.println(response);
    }

    @Test
    void testStream(@Autowired DashScopeChatModel chatModel) {
        Flux<String> response = chatModel.stream("你好");
        response.toIterable().forEach(subResult -> {
            System.out.println(subResult);
        });
    }

//    @Test
//    void testSingleModel(@Autowired ChatClient.Builder builder) {
//        ChatClient chatClient = builder.build();
//        String result = chatClient.prompt()
//                .user("你好")
//                .call()
//                .content();
//        System.out.println(result);
//    }

    @Test
    void testMutlModel(@Autowired DashScopeChatModel chatModel) {
        ChatClient chatClient = ChatClient.create(chatModel);

        String result = chatClient.prompt()
                .user("你好")
                .call()
                .content();
        System.out.println(result);
    }

    @Test
    void testSystemPrompt(@Autowired DashScopeChatModel chatModel, @Value("classpath:/file/prompet.sd") Resource resource) {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(resource)
                .defaultOptions(ChatOptions.builder().temperature(0.1).build())
                .build();

        String result = chatClient.prompt()
                .system("限制50token")
                .user("你好,spring ai alibaba对spring ai做了哪些升级")
                .call()
                .content();
        System.out.println(result);
    }

    @Test
    void testAdvisor(@Autowired DashScopeChatModel chatModel){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder().temperature(0.1).build())
                .build();

        String result = chatClient.prompt()
                .advisors(new ReAgainAdvisor(), new SimpleLoggerAdvisor(), new SafeGuardAdvisor(Lists.newArrayList("绝密"), "系统 404", 0))
//                .system("限制50token")
                .user("你好")
                .call()
                .content();

        System.out.println(result);

    }
}
