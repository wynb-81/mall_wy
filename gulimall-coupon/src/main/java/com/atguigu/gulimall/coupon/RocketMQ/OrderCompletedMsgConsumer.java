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
        topic = "ORDER_TOPIC_COMPLETED",
        consumerGroup = "coupon_order_completed",
        consumeMode = ConsumeMode.CONCURRENTLY,
        maxReconsumeTimes = 3
)
public class OrderCompletedMsgConsumer implements RocketMQListener<MessageExt> {
    private final RedisTemplate<String, String> redisTemplate;
    private final CouponService couponService;
    private final OrderMsgHandler orderMsgHandler;

    public OrderCompletedMsgConsumer(
            RedisTemplate<String, String> redisTemplate,
            CouponService couponService, OrderMsgHandler orderMsgHandler) {
        this.redisTemplate = redisTemplate;
        this.couponService = couponService;
        this.orderMsgHandler = orderMsgHandler;
    }

    /**
     * 消费入口
     * @author wynb
     * @date 2026/1/20 16:06
     */
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
            completedCoupon(message);

            //3.标记消息已处理，幂等记录
            String key = orderMsgHandler.buildProcessedKey(message);
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);

            log.info("消息消费成功。orderSn:{}",orderSn);
        }catch (Exception e){
            log.error("系统异常，触发重试。orderSn:{}",orderSn,e);
            throw new RuntimeException("消费失败，触发重试",e);
        }
    }

    /**
     * 核销优惠券
     * @author wynb
     * @date 2026/1/20 18:27
     */
    private void completedCoupon(OrderMessage message) throws BusinessException {
        if(message.getCouponId() == null){
            log.info("订单未使用优惠券，跳过.orderSn:{}",message.getOrderSn());
            return;
        }

        boolean completed =  couponService.completedCoupon(message.getUserId(), message.getCouponId(), message.getOrderSn());
        if(!completed){
            throw new BusinessException("核销优惠券失败");
        }
        log.info("优惠券核销成功。orderSn:{}，couponId:{}",message.getOrderSn(),message.getCouponId());

    }
}
