package com.zxx.springaitest.service;

import reactor.core.publisher.Flux;

public interface SmartCustomerService {

    /**
     * 处理用户对话
     * @param userMessage 用户输入
     * @param conversationId 会话ID（用于隔离不同用户的记忆）
     */
    Flux<String> chatWithStream(String userMessage, String conversationId);

    String chat(String userMessage, String conversationId);
}