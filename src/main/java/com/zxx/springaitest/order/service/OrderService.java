package com.zxx.springaitest.order.service;

import com.zxx.springaitest.order.dao.OrderRepository;
import com.zxx.springaitest.order.enums.OrderStatusEnum;
import com.zxx.springaitest.order.model.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order queryOrder(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
    }

    public Order cancelOrder(String orderId) {
        Order order = queryOrder(orderId);
        if (order.status() != OrderStatusEnum.PENDING_SHIPMENT) {
            throw new IllegalStateException("仅待发货订单可取消，当前状态: " + order.status().getDescription());
        }
        if (orderRepository.cancelPending(orderId) == 0) {
            throw new IllegalStateException("订单状态已变化，请刷新后重试");
        }
        return queryOrder(orderId);
    }
}
