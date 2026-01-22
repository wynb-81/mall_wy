package com.atguigu.gulimall.ware.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLockMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID（用于幂等性校验）
     */
    private String messageId;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 订单项列表
     */
    private List<OrderItemMessage> orderItems;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 业务标识（防止重复消费）
     */
    private String bizKey;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemMessage implements Serializable {
        private Long skuId;
        private Integer count;
        private String skuName;
    }
}