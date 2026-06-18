package com.zxx.springaitest.controller;

import com.zxx.springaitest.service.KnowledgeBaseService;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-base")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @Autowired
    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/insert-text")
    public ResponseEntity<String> insertTextContent(@RequestParam("content") String content) {
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文本内容是必需的");
        }
        try {
            knowledgeBaseService.insertTextContent(content);
            return ResponseEntity.ok("文本内容已成功插入");
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("插入文本内容失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFileByType(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("文件为空");
        }
        try {
            String result = knowledgeBaseService.loadFileByType(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> similaritySearch(@RequestParam("query") String query,
                                              @RequestParam(value = "topK", defaultValue = "5") int topK) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("查询内容是必需的");
        }
        if (topK <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("topK必须是正整数");
        }
        try {
            List<Document> results = knowledgeBaseService.similaritySearch(query, topK);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("相似性搜索过程中发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chatWithKnowledge(@RequestParam("query") String query,
                                                    @RequestParam(value = "topK", defaultValue = "5") int topK) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("查询问题是必需的");
        }
        if (topK <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("topK必须是正整数");
        }
        try {
            String answer = knowledgeBaseService.chatWithKnowledge(query, topK);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("对话过程中发生错误: " + e.getMessage());
        }
    }

    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chatWithKnowledgeStream(@RequestParam("query") String query,
                                                                @RequestParam(value = "topK", defaultValue = "5") int topK) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Flux.just("查询问题是必需的"));
        }
        if (topK <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Flux.just("topK必须是正整数"));
        }
        try {
            Flux<String> answerStream = knowledgeBaseService.chatWithKnowledgeStream(query, topK);
            return ResponseEntity.ok(answerStream);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Flux.just("流式对话过程中发生错误: " + e.getMessage()));
        }
    }
}