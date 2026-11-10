package com.zxx.springaitest.order.enums;

public enum OrderStatusEnum {

    PENDING_SHIPMENT(1, "待发货"),
    SHIPPED(2, "已发货"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    OrderStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatusEnum fromCode(int code) {
        for (OrderStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}
