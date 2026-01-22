package com.atguigu.gulimall.order.event;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

import static com.atguigu.gulimall.order.enume.OrderStatusEnum.UNPAID;

@Component
@Slf4j
@RocketMQMessageListener(
        topic = "ORDER_EVENTS_TOPIC",
        consumerGroup = "payment-timeout-consumer",
        selectorExpression = "PAYMENT_CHECK"
)
public class PaymentTimeoutConsumer {

    private final OrderServiceImpl orderService;
    private final OrderCancelService orderCancelService;

    public PaymentTimeoutConsumer(OrderServiceImpl orderService, OrderCancelService orderCancelService) {
        this.orderService = orderService;
        this.orderCancelService = orderCancelService;
    }

    public void checkPaymentTimeout(OrderEvent event) throws BusinessException {
        String orderSn = event.getOrderSn();
        log.info("订单:{}支付超时", orderSn);
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);
        if(order == null){
            return;
        }

        //仍然是待支付状态，自动取消
        if(order.getStatus() == UNPAID.getCode()){
            orderCancelService.cancelOrder(orderSn, OrderCancelService.CancelType.TIMEOUT);
        }
    }
}
