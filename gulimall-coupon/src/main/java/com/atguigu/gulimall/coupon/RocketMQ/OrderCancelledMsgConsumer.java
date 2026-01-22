package com.atguigu.gulimall.coupon.RocketMQ;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.gulimall.coupon.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RocketMQMessageListener(
        topic = "ORDER_TOPIC_CANCELLED",
        consumerGroup = "coupon_order_cancelled",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
public class OrderCancelledMsgConsumer implements RocketMQListener<MessageExt> {
    private final RedisTemplate<String, String> redisTemplate;
    private final CouponService couponService;
    private final OrderMsgHandler orderMsgHandler;

    public OrderCancelledMsgConsumer(
            RedisTemplate<String, String> redisTemplate,
            CouponService couponService, OrderMsgHandler orderMsgHandler) {
        this.redisTemplate = redisTemplate;
        this.couponService = couponService;
        this.orderMsgHandler = orderMsgHandler;
    }

    @Override
    public void onMessage(MessageExt rocketMsg) {
        OrderMessage message = orderMsgHandler.parseMessage(rocketMsg);
        String orderSn = message.getOrderSn();
        String msgId = rocketMsg.getMsgId();

        log.info("收到订单消息，开始处理。orderSn:{},msgId:{}", orderSn, msgId);

        try{
            //1.幂等性检查，防止重复消费
            if(orderMsgHandler.isMessageProcessed(message)){
                log.info("消息已处理，幂等返回。orderSn:{}",orderSn);
            }

            //2.锁定优惠券
            unlockCoupon(message);

            //3.标记消息已处理，幂等记录
            String key = orderMsgHandler.buildProcessedKey(message);
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);

            log.info("消息消费成功。orderSn:{}",orderSn);
        }catch (Exception e){
            log.error("系统异常，触发重试。orderSn:{}",orderSn,e);
            throw new RuntimeException("消费失败，触发重试",e);
        }
    }


    private void unlockCoupon(OrderMessage message) throws BusinessException {
        if(message.getCouponId() == null){
            log.info("订单未使用优惠券，跳过.orderSn:{}",message.getOrderSn());
            return;
        }

        boolean unlocked =  couponService.unlockCoupon(message.getUserId(), message.getCouponId(), message.getOrderSn());
        if(!unlocked){
            throw new BusinessException("解锁优惠券失败");
        }
        log.info("优惠券解锁成功。orderSn:{}，couponId:{}",message.getOrderSn(),message.getCouponId());

    }
}
