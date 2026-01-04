package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;


@Configuration
public class MyMQConfig {
    /**
     * Bean能够让容器中的Queue、Exchange、Binding自动创建（RabbitMQ没有的情况）
     * 如果已经在MQ中创建一次了，那么即使修改下面配置中的属性，也不会覆盖MQ中之前的属性。只能去MQ手动删除
     * @author wynb-81
     * @create 2025/6/24
     **/
    @Bean
    public Queue orderDelayQueue(){
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        //String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments
        return new Queue("order.delay.queue", true, false, false, arguments);
    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        return new Queue("order.release.order.queue", true, false, false);
    }

    @Bean
    public Exchange orderEventExchange(){
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange("order-event-exchange",true,false);
    }

    @Bean
    public Binding orderCreateOrderBinding(){
        //String destination, DestinationType destinationType, String exchange,
        // String routingKey, @Nullable Map<String, Object> arguments)
        return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
    }

    @Bean
    public Binding orderReleaseOrderBinging(){
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
    }

    /**
     * 绑定订单系统的交换机和库存系统的队列(订单释放和库存释放)
     * @author wynb-81
     * @create 2025/6/25
     **/
    @Bean
    public Binding orderReleaseOtherBinging(){
        return new Binding("order.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order.#",
                null);
    }

    /**
     * 秒杀订单削峰队列
     * @author wynb-81
     * @create 2025/6/26
     **/
    @Bean
    public Binding orderSeckillOrderQueueBinding(){
        return new Binding("order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);
    }

}
