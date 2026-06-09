package com.zxx.springaitest;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@RestController
public class MorePlatformAndModelController {

    HashMap<String, ChatModel> modelMap = new HashMap<>();

    public MorePlatformAndModelController(DashScopeChatModel dashScopeChatModel,
                                          DeepSeekChatModel deepSeekChatModel) {
        modelMap.put("dashscope", dashScopeChatModel);
        modelMap.put("deepseek", deepSeekChatModel);
    }

    /**
     * http://127.0.0.1:8080/chat?message=%E4%BD%A0%E5%A5%BD&plaform=dashscope&model=deepseek-v4-pro&temperature=0.1
     * @param message
     * @param options
     * @return
     */
    @RequestMapping(value = "chat", produces = "text/stream;charset=UTF-8")
    public Flux<String> chat(String message, MorePlatformAndModelOptions options) {
        String plaform = options.getPlaform();

        ChatModel model = modelMap.get(plaform);
        ChatClient.Builder builder = ChatClient.builder(model);
        ChatClient chatClient = builder.
                defaultOptions(
                        ChatOptions.builder()
                                .temperature(options.getTemperature())
                                .model(options.getModel())
                                .build()
                ).build();

        return chatClient.prompt().user(message).stream().content();
     }
}
