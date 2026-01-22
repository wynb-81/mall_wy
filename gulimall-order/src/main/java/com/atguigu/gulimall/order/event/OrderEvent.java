package com.atguigu.gulimall.order.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrderEvent {
    private String eventId = UUID.randomUUID().toString();
    private String orderSn;
    private Long userId;
    private String eventType; // ORDER_CREATED, ORDER_CANCELLED, ORDER_COMPLETED
    private LocalDateTime eventTime = LocalDateTime.now();

    // 静态工厂方法
    public static OrderEvent created(String orderSn, Long userId, Long couponId) {
        OrderEvent event = new OrderEvent();
        event.setOrderSn(orderSn);
        event.setUserId(userId);
        event.setEventType("CREATED");
        return event;
    }

    public static OrderEvent cancelled(String orderSn, Long userId) {
        OrderEvent event = new OrderEvent();
        event.setOrderSn(orderSn);
        event.setUserId(userId);
        event.setEventType("CANCELLED");
        return event;
    }

    public static OrderEvent completed(String orderSn, Long userId) {
        OrderEvent event = new OrderEvent();
        event.setOrderSn(orderSn);
        event.setUserId(userId);
        event.setEventType("COMPLETED");
        return event;
    }
}
