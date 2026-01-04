package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;


@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter(){
        return  new Jackson2JsonMessageConverter();
    }

    /**
     * 定制rabbitTemplate
     * 1.服务器收到消息就回调
     *       1.spring.rabbitmq.publisher-confirms=true
     *       2.设置确认回调
     * 2.消息正确抵达队列，进行回调
     *      1.spring.rabbitmq.publisher-returns=true
     *        spring.rabbitmq.template.mandatory=true
     *      2.设置确认回调
     * @author wynb-81
     * @create 2025/6/9
     **/
    @PostConstruct //MyRabbitConfig对象创建完成以后，执行这个方法
    public void initRabbitTemplate(){
        //设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * @Param correlationData 当前消息的唯一关联数据（消息的唯一id）
             * @Param ack 消息是否成功收到
             * @Param cause 失败的原因
             * @author wynb-81
             * @create 2025/6/9
             **/
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                //服务器收到了
                System.out.println("confirm..correlationData["+correlationData+"]==>ack["+ack+"]==>cause["+cause+"]");
            }
        });

//        设置消息抵达队列的确认回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            /**
             * @Param returnedMessage，包含了
             * message 投递失败的消息详细信息
             * replyCode 回复的状态码
             * replyText 恢复的文本内容
             * exchange 当时这个消息发送给哪个交换机
             * routingKey 当时这个消息用哪个路由键
             * @author wynb-81
             * @create 2025/6/9
             **/

            @Override
            public void returnedMessage(ReturnedMessage message) {
                //报错误了，修改数据库当前消息的状态->错误
                System.out.println("Fail message["+message.getMessage()
                        +"]==>replyCode["+message.getReplyCode()
                        +"]==>replyText["+message.getReplyText()
                        +"]==>routingKey["+message.getRoutingKey()
                        +"]==>exchange["+message.getExchange());

            }
        });

    }
}
