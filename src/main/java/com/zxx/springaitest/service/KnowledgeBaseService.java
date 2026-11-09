package com.zxx.springaitest.service;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

public interface KnowledgeBaseService {

    void insertTextContent(String content);

    String loadFileByType(MultipartFile file);

    List<Document> similaritySearch(String query, int topK);

    String chatWithKnowledge(String query, int topK);

    Flux<String> chatWithKnowledgeStream(String query, int topK);

    String chatWithKnowledge(String query, String conversationId);
}