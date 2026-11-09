//package com.zxx.springaitest.tools;
//
//import com.zxx.springaitest.order.Order;
//import com.zxx.springaitest.order.OrderService;
//import com.zxx.springaitest.order.OrderStatusEnum;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.Objects;
//
//@Component
//public class OrderTools {
//
//    @Autowired
//    private OrderService orderService;
//
//    // 模拟订单查询
//    @Tool(description = "根据订单号查询订单状态、商品明细和收货地址。")
//    public String queryOrder(@ToolParam(description = "电商平台订单ID") String orderId) {
//        // 实际开发中这里调用数据库或微服务
//        Order order = orderService.queryOrder(orderId);
//        return String.format("订单 %s 状态为：%s，预计明天送达。", order.orderId(), order.status().getDescription());
//    }
//
//    // 模拟订单取消
//    @Tool(description = "根据订单号取消订单。仅当订单处于'待发货'状态时允许取消。")
//    public String cancelOrder(@ToolParam(description = "需要取消的订单ID") String orderId) {
//        orderService.cancelOrder(orderId);
//        return String.format("订单 %s 已成功取消，款项将在1-3个工作日内原路退回。", orderId);
//    }
//}