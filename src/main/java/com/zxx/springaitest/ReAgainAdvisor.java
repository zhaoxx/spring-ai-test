package com.zxx.springaitest;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

public class ReAgainAdvisor implements BaseAdvisor {

    private static final String DEFAULT_USER_TEXT_ADVISE = """
            {user_input_text}
            再思考一下上个问题：{user_input_text}
        """;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        //
        String contents = chatClientRequest.prompt().getContents();
        String reContents = PromptTemplate.builder().template(DEFAULT_USER_TEXT_ADVISE).build().render(Map.of("user_input_text", contents));

        ChatClientRequest clientRequest = chatClientRequest.mutate()
                .prompt(Prompt.builder().content(reContents).build())
                .build();
        return clientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
