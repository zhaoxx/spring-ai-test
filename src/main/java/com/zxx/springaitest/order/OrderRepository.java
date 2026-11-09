package com.zxx.springaitest.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private static final String TABLE = "orders";

    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> new Order(
            rs.getString("order_id"),
            rs.getString("orderer"),
            OrderStatusEnum.fromCode(rs.getInt("status")),
            rs.getTimestamp("create_time").toLocalDateTime(),
            rs.getTimestamp("update_time").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Order> findById(String orderId) {
        List<Order> orders = jdbcTemplate.query(
                "SELECT order_id, orderer, status, create_time, update_time FROM " + TABLE + " WHERE order_id = ?",
                ROW_MAPPER, orderId);
        return orders.stream().findFirst();
    }

    /**
     * 仅当订单处于待发货状态时更新为已取消，返回受影响行数。
     */
    public int cancelPending(String orderId) {
        return jdbcTemplate.update(
                "UPDATE " + TABLE + " SET status = ?, update_time = NOW() WHERE order_id = ? AND status = ?",
                OrderStatusEnum.CANCELLED.getCode(), orderId, OrderStatusEnum.PENDING_SHIPMENT.getCode());
    }
}
