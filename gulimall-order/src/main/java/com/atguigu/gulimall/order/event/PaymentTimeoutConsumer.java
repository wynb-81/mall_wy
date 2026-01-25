package com.atguigu.gulimall.order.event;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import static com.atguigu.gulimall.order.enume.OrderStatusEnum.UNPAID;

@Component
@Slf4j
@RocketMQMessageListener(
        topic = "ORDER_EVENTS_TOPIC",
        consumerGroup = "payment-timeout-consumer",
        selectorExpression = "PAYMENT_CHECK"
)
public class PaymentTimeoutConsumer implements RocketMQListener<OrderEvent> {

    private final OrderServiceImpl orderService;
    private final OrderCancelService orderCancelService;

    public PaymentTimeoutConsumer(OrderServiceImpl orderService, OrderCancelService orderCancelService) {
        this.orderService = orderService;
        this.orderCancelService = orderCancelService;
    }



    @Override
    public void onMessage(OrderEvent event) {
        String orderSn = event.getOrderSn();
        log.info("订单:{}支付超时", orderSn);
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);
        if(order == null){
            return;
        }

        //仍然是待支付状态，自动取消
        if(order.getStatus() == UNPAID.getCode()){
            try {
                orderCancelService.cancelOrder(orderSn, OrderCancelService.CancelType.TIMEOUT);
            } catch (BusinessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
