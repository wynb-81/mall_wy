package com.atguigu.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "stock.release.stock.queue")
@Service
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 1、库存自动解锁：下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     * 2、订单失败：锁库存失败，导致回滚，拿不到工作单信息
     *
     * 如果解锁库存的消息失败，一定要告诉MQ解锁失败，不要删除这个消息，解决办法：启动手动ack
     spring.rabbitmq.listener.simple.acknowledge-mode=manual
     *
     * @author wynb-81
     * @create 2025/6/24
     **/
    @RabbitHandler
    public void handleStockLockedRelease(StockLockTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息");
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            //有任何异常，就说明解锁失败了。
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }


    }

    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭准备解锁库存");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            //有任何异常，就说明解锁失败了。
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }
}
