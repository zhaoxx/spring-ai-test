package com.zxx.springaitest.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicToolService {

    record Order(String orderId, String name) {
        public Order(String orderId, String name) {
            this.orderId = orderId;
            this.name = name;
        }

        public Order() {
            this(null, null);
        }
    }

    /**
     * 根据订单号查询订单状态、商品明细和收货地址。
     * @param orderId
     * @return
     */
    public String queryOrder(String orderId) {
        // 实际开发中这里调用数据库或微服务
        if (orderId.startsWith("abc")) {
            return String.format("订单 %s 状态为：待发货", orderId);
        }
        return String.format("订单 %s 状态为：已发货，商品：[蓝牙耳机x1]，预计明天送达。", orderId);
    }

    /**
     * 根据订单号取消订单。仅当订单处于'待发货'状态时允许取消。
     * @param orderId
     * @param name
     * @return
     */
    public String cancelOrder(String orderId, String name) {
        // 实际开发中需做严格的幂等和状态校验
        if ("abc".equals(orderId)) {
            throw new RuntimeException("参数异常");
        }
        return String.format("订单 %s 已成功取消，款项将在1-3个工作日内原路退回。", orderId);
    }

    public List<ToolCallback> getTools(DynamicToolService dynamicToolService) {
        List<ToolCallback> callbackList = new ArrayList<>();

        callbackList.add(this.getCancelOrderToolCallback(dynamicToolService));
        callbackList.add(this.getQueryOrderToolCallback(dynamicToolService));

        return callbackList;

    }

    /**
     * 封装取消订单的callback
     * @param dynamicToolService
     * @return
     */
    private ToolCallback getCancelOrderToolCallback(DynamicToolService dynamicToolService) {
        // 1、从DB中获取tools配置；实际开发中，这里可以是一个 for 循环遍历数据库查询出的工具列表
        String toolName = "cancelOrder";
        String toolDescription = "根据订单号取消订单。仅当订单处于'待发货'状态时允许取消。" +
                "需要传入一个包含 orderId(订单ID) 和 name(真实人名) 的JSON对象。";
        String inputSchema = """
            {
              "type": "object",
              "properties": {
                "orderId": { "type": "string", "description": "订单ID" },
                "name": { "type": "string", "description": "真实人名" }
              },
              "required": ["orderId", "name"]
            }
            """;

        // 2. 通过反射获取要执行的业务方法
        Method method = ReflectionUtils.findMethod(
                DynamicToolService.class, "cancelOrder", String.class, String.class
        );

        // 3. 构建 ToolDefinition
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name(toolName)
                .description(toolDescription)
                .inputSchema(inputSchema)
                .build();

        // 4. 构建 ToolCallback（注意：必须传入 Spring Bean，不能直接 new。否则无法实现依赖注入）
        return MethodToolCallback.builder()
                .toolDefinition(toolDefinition)
                .toolMethod(method)
                .toolObject(dynamicToolService)
                .build();
    }

    /**
     * 封装查询订单的callback
     * @param dynamicToolService
     * @return
     */
    private ToolCallback getQueryOrderToolCallback(DynamicToolService dynamicToolService) {
        // 1、从DB中获取tools配置；实际开发中，这里可以是一个 for 循环遍历数据库查询出的工具列表
        String toolName = "queryOrder";
        String toolDescription = "根据订单号查询订单状态、商品明细和收货地址。" +
                "需要传入一个包含 orderId(订单ID) 的JSON对象。";
        String inputSchema = """
            {
              "type": "object",
              "properties": {
                "orderId": { "type": "string", "description": "订单ID" },
                "name": { "type": "string", "description": "真实人名" }
              },
              "required": ["orderId"]
            }
            """;

        // 2. 通过反射获取要执行的业务方法
        Method method = ReflectionUtils.findMethod(
                DynamicToolService.class, "queryOrder", String.class
        );

        // 3. 构建 ToolDefinition
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name(toolName)
                .description(toolDescription)
                .inputSchema(inputSchema)
                .build();

        // 4. 构建 ToolCallback（注意：必须传入 Spring Bean，不能直接 new。否则无法实现依赖注入）
        return MethodToolCallback.builder()
                .toolDefinition(toolDefinition)
                .toolMethod(method)
                .toolObject(dynamicToolService)
                .build();
    }

}
