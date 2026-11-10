package com.zxx.springaitest.order.model;

import com.zxx.springaitest.order.enums.OrderStatusEnum;

import java.time.LocalDateTime;

public record Order(
        String orderId,
        String orderer,
        OrderStatusEnum status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
