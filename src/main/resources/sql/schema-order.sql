CREATE TABLE IF NOT EXISTS `orders` (
    `order_id` varchar(36) NOT NULL COMMENT '订单号',
    `orderer` varchar(64) NOT NULL COMMENT '下单人',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=待发货，2=已发货，3=已取消',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

INSERT IGNORE INTO `orders` (`order_id`, `orderer`, `status`, `create_time`, `update_time`) VALUES
('10001', '张三', 1, NOW(), NOW()),
('10002', '李四', 2, NOW(), NOW());
