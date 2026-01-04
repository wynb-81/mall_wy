package com.atguigu.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class MyRabbitConfig {
    //使用JSON序列化机制进行消息转换
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
//    /**
//     * 如果没有监听，那么rabbit不会创建下面的队列，这也是之前为啥我order的队列都没创建的原因
//     * rabbit只会在第一次启动的时候创建。所以放上一个监听，这样就能让这些队列在rabbit已经启动之后创建了
//     * 但是创建之后就要关掉这个监听，否则在测试的时候，会有两个消费。正常我提交一次订单，就只有一次消费。
//     * @author wynb-81
//     * @create 2025/6/24
//     **/
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Message message){
//
//    }

    //创建交换机
    @Bean
    public Exchange stockEventExchange(){
        return new TopicExchange("stock-event-exchange",true,false);
    }

    //创建队列
    @Bean
    public Queue stockReleaseStockQueue(){
        return new Queue("stock.release.stock.queue",true,false,false);
    }

    @Bean
    public Queue stockDelayQueue(){
        /*
         * "x-dead-letter-exchange" 死信路由
         * "x-dead-letter-routing-key"  路由键
         * "x-message-ttl"  过期时间
         */
        HashMap<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange","stock-event-exchange");
        args.put("x-dead-letter-routing-key","stock.release");
        args.put("x-message-ttl",120000);
        return new Queue("stock.delay.queue",true,false,false,args);
    }

    @Bean
    public Binding stockReleaseBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

    @Bean
    public Binding stockLockedBinding(){
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }
}
