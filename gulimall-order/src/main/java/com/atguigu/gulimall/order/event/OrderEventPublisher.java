package com.atguigu.gulimall.order.event;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.rocketMQ.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderEventPublisher {
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderMessageService orderMessageService;

    public OrderEventPublisher(RocketMQTemplate rocketMQTemplate, OrderMessageService orderMessageService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderMessageService = orderMessageService;
    }

    /**
     * 订单创建事件
     * @author wynb
     * @date 2026/1/21 16:35
     */
    public void publishOrderCreated(String orderSn,Long userId,Long couponId) {
        OrderEvent event = OrderEvent.created(orderSn, userId, couponId);
        sendEvent(event);
        log.info("发布订单创建事件: orderSn={}, couponId={}", orderSn, couponId);

        // 下单时同时发送30分钟延迟消息，用于超时检查
        scheduleTimeoutCheck(orderSn, userId);
    }


    /**
     * 订单取消事件
     * @author wynb
     * @date 2026/1/21 16:35
     */
    //TODO 订单取消，解锁库存的服务应该也是异步的
    public void publishOrderCancelled(String orderSn, Long userId) {
        OrderEvent event = OrderEvent.cancelled(orderSn, userId);
        sendEvent(event);
        log.info("发布订单取消事件: orderSn={}", orderSn);
    }

    /**
     * 订单完成事件
     * @author wynb
     * @date 2026/1/21 16:35
     */
    public void publishOrderCompleted(String orderSn, Long userId) {
        OrderEvent event = OrderEvent.completed(orderSn, userId);
        sendEvent(event);
        log.info("发布订单完成事件: orderSn={}", orderSn);
    }

    private void sendEvent(OrderEvent event) {
        String fullTopic = "ORDER_TOPIC" + event.getEventType();    //ORDER_TOPIC_CREATED等等
        OrderEntity order = new OrderEntity();
        order.setOrderSn(event.getOrderSn());
        order.setMemberId(event.getUserId());
        orderMessageService.sendMessageAfterCommit(order,fullTopic);
    }

    /**
     * 延迟消息：超时支付检查
     * @author wynb
     * @date 2026/1/21 16:41
     */
    private void scheduleTimeoutCheck(String orderSn, Long userId) {
        OrderEvent event = new OrderEvent();
        event.setOrderSn(orderSn);
        event.setUserId(userId);
        event.setEventType("PAYMENT_TIMEOUT_CHECK");

        rocketMQTemplate.syncSendDelayTimeSeconds(
                "ORDER_EVENTS_TOPIC:PAYMENT_CHECK",
                event,
                60*3
        );
        log.info("已安排支付超时检查：3分钟后，orderSn={}, userId={}", orderSn, userId);
    }

}
