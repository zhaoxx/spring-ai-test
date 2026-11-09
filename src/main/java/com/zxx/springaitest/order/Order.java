package com.zxx.springaitest.order;

import java.time.LocalDateTime;

public record Order(
        String orderId,
        String orderer,
        OrderStatusEnum status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
