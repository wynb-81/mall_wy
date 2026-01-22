package com.atguigu.gulimall.coupon.RocketMQ;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class OrderMsgHandler {
    private final RedisTemplate<String, String> redisTemplate;

    public OrderMsgHandler(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 幂等性检查
     * @author wynb
     * @date 2026/1/20 16:31
     */
    public boolean isMessageProcessed(OrderMessage message) {
        String key = buildProcessedKey(message);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public String buildProcessedKey(OrderMessage message) {
        // 使用 业务类型+订单号+优惠券ID 作为唯一标识
        return String.format("msg:processed:coupon:%s:%s",
                message.getOrderSn(),
                message.getCouponId() != null ? message.getCouponId() : "null");
    }

    /**
     * 解析消息
     * @author wynb
     * @date 2026/1/20 16:30
     */
    public OrderMessage parseMessage(MessageExt rocketMsg) {
        String body = new String(rocketMsg.getBody(), StandardCharsets.UTF_8);
        return JSON.parseObject(body, OrderMessage.class);
    }
}
