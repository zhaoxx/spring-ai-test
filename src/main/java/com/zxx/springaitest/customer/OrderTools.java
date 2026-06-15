package com.zxx.springaitest.customer;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class OrderTools {

    // 模拟订单查询
    @Tool(description = "根据订单号查询订单状态、商品明细和收货地址。")
    public String queryOrder(@ToolParam(description = "电商平台订单号") String orderId) {
        // 实际开发中这里调用数据库或微服务
        if (orderId.startsWith("abc")) {
            return String.format("订单 %s 状态为：待发货", orderId);
        }
        return String.format("订单 %s 状态为：已发货，商品：[蓝牙耳机x1]，预计明天送达。", orderId);
    }

    // 模拟订单取消
    @Tool(description = "根据订单号取消订单。仅当订单处于'待发货'状态时允许取消。")
    public String cancelOrder(@ToolParam(description = "需要取消的订单号") String orderId) {
        // 实际开发中需做严格的幂等和状态校验
        if ("abc".equals(orderId)) {
            throw new RuntimeException("参数异常");
        }
        return String.format("订单 %s 已成功取消，款项将在1-3个工作日内原路退回。", orderId);
    }
}