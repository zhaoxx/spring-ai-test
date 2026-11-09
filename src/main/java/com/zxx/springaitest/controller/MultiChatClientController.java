package com.zxx.springaitest.controller;

import com.zxx.springaitest.model.AIJob;
import com.zxx.springaitest.model.CustomerOptions;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/api/multi")
public class MultiChatClientController {

    @Autowired
    private ChatClient planningChatClient;

    @Autowired
    private ChatClient botChatClient;

    @GetMapping(value = "/chat", produces = "text/stream;charset=UTF-8")
    public Flux<String> chat(CustomerOptions customerOptions) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        // 推送消息
        sink.tryEmitNext("正在规划任务...<br/>");

        new Thread(() ->{
            AIJob.Job job = planningChatClient.prompt().user(customerOptions.getMessage())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, customerOptions.getSessionId()))
                    .call().entity(AIJob.Job.class);

            System.out.println(job);
            switch (job.jobType()) {
                case CANCEL -> {
                    // todo 执行取消业务
                    if (StringUtils.isBlank(job.orderNo()) || StringUtils.isBlank(job.name())) {
                        sink.tryEmitNext("请输入姓名和订单号");
                    } else {
                        sink.tryEmitNext("取消成功");
                    }
                }
                case QUERY -> {
                    if (StringUtils.isBlank(job.orderNo())) {
                        sink.tryEmitNext("请输入订单号");
                    } else {
                        sink.tryEmitNext("正在查询订单信息...<br/>");
                    }
                }
                case OTHER -> {
                    Flux<String> content = botChatClient.prompt().user(customerOptions.getMessage())
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, customerOptions.getSessionId())).stream().content();
                    content.doOnNext(sink::tryEmitNext)
                            .doOnComplete(() -> sink.tryEmitComplete())
                            .subscribe();
                }
                default -> {
                    sink.tryEmitNext("解析异常");
                }
            }
        }).start();

        return sink.asFlux();
    }
}
