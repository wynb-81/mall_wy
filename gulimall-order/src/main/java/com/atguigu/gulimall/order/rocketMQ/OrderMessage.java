package com.atguigu.gulimall.order.rocketMQ;

import lombok.Data;

import java.util.Date;

@Data
public class OrderMessage {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券ID（可能为null）
     */
    private Long couponId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 消息ID（用于幂等）
     */
    private String messageId;

}
