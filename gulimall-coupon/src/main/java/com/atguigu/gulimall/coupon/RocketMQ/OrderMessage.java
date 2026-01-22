package com.atguigu.gulimall.coupon.RocketMQ;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class OrderMessage {
    // 消息唯一标识
    private String messageId = UUID.randomUUID().toString();
    // 订单号（业务唯一键）
    private String orderSn;
    // 用户ID
    private Long userId;
    // 优惠券ID（可为空）
    private Long couponId;
    // 使用积分
    private Integer useIntegration;
    // 事件类型（便于扩展）
    private String eventType = "ORDER_CREATED";
    // 订单状态（发送时的快照）
    private Integer orderStatus;
    // 创建时间
    private Date createTime = new Date();
}

/*
* # application.yml
rocketmq:
  name-server: 127.0.0.1:9876
  consumer:
    coupon-group:  # 优惠券服务消费者组
      name: coupon-service-group
      topic: ORDER_CREATED_TOPIC
      consume-mode: CONCURRENTLY  # 并发模式
      message-model: CLUSTERING   # 集群模式（负载均衡）
* */