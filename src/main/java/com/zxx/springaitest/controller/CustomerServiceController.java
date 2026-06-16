package com.zxx.springaitest.controller;

import com.zxx.springaitest.model.CustomerOptions;
import com.zxx.springaitest.service.SmartCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
public class CustomerServiceController {

    @Autowired
    private SmartCustomerService smartCustomerService;

    /**
     * http://localhost:8080/api/customer/chat?message=取消订单id为11111的订单&sessionId=1111
     * @param customerOptions
     * @return
     */
    @RequestMapping(value = "/chat", produces = "text/stream;charset=UTF-8")
    public Flux<String> chat(CustomerOptions customerOptions) {
        String message = customerOptions.getMessage();
        String sessionId = Objects.isNull(customerOptions.getSessionId()) ? UUID.randomUUID().toString() : customerOptions.getSessionId();
        return smartCustomerService.chat(message, sessionId);
    }
}