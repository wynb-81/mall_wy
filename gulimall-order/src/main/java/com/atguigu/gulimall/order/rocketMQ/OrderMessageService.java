package com.atguigu.gulimall.order.rocketMQ;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Slf4j
public class OrderMessageService {
    private final RocketMQTemplate rocketMQTemplate;
    private final MessageRetryRecordService retryRecordService;

    public OrderMessageService(RocketMQTemplate rocketMQTemplate, MessageRetryRecordService retryRecordService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.retryRecordService = retryRecordService;
    }

    private final BlockingDeque<RetryTask> retryQueue = new LinkedBlockingDeque<>(1000);
    /*
    Executors.newSingleThreadExecutor() 是 Java 中的一个线程池工具，它创建一个单线程化的线程池。
    这意味着它只会使用一个工作线程来执行任务，确保所有任务都按照提交的顺序（FIFO，LIFO，优先级）执行。
    这种线程池的优点是可以串行执行所有任务，如果这个唯一的线程因为异常结束，会有一个新的线程来替代它，保证任务的执行顺序。
    */
    /*
    * SingleThreadExecutor使用LinkedBlockingDeque，可能会导致OOM
    * 但是指定了大小，并且大小在可控范围内的话，基本可以杜绝OOM的问题
    * 同时在消息入队的时候，使用的是retryQueue.offer(retryTask);
    * 如果队列满了会直接返回false，因此应该不会导致OOM
    * */
    private final ExecutorService retryExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        // 启动内存重试消费者
        retryExecutor.submit(this::consumeRetryQueue);
    }

    /**
     * 消费内存重试队列
     * @author wynb
     * @date 2026/1/19 18:12
     */
    private void consumeRetryQueue() {
        try{
            RetryTask task = retryQueue.take(); //阻塞获取重试消息
            //进行延时重试，重试次数越多，延迟越长
            int delaySeconds = task.retryCount * 5; //0,5,10s
            if(delaySeconds > 0){
                TimeUnit.SECONDS.sleep(delaySeconds);
            }
            sendMessageDirectly(task.order,task.topic, task.retryCount);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.warn("内存重试队列消费者被中断",e);
        }catch (Exception e){
            log.error("处理内存重试任务异常",e);
        }
    }

    /**
     * 在事务提交后发送消息
     * @author wynb
     * @date 2026/1/19 17:39
     */
    public void sendMessageAfterCommit(OrderEntity order,String topic) {
        // 检查是否处于活跃事务中
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn("当前无活跃事务，直接发送消息。orderSn:{}", order.getOrderSn());
            sendMessageDirectly(order, topic, 0);
            return;
        }

        //注册事务同步回调：事务提交后执行
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendMessageDirectly(order, topic, 0);
            }

            @Override
            public void afterCompletion(int status) {
                // 事务完成后可选的日志记录
                if (status == STATUS_ROLLED_BACK) {
                    log.info("订单事务已回滚，取消消息发送。orderSn:{}", order.getOrderSn());
                }
            }
        });
    }

    /**
     * 直接发送消息，有三重重试机制
     * @author wynb
     * @date 2026/1/19 17:45
     */
    private void sendMessageDirectly(OrderEntity order, String topic, int retryCount) {
        OrderMessage message = buildMessage(order);
        String messageKey = order.getOrderSn();
        try{
            SendResult sendResult = rocketMQTemplate.syncSend(
                    topic,
                    MessageBuilder.withPayload(JSON.toJSONString(message)).setHeader(RocketMQHeaders.KEYS,messageKey).build(),
                    3000);
        log.info("消息发送成功！orderSn:{}，msgId:{}", order.getOrderSn(),sendResult.getMsgId());
        }catch (Exception e){
            log.error("消息发送失败，进入重试。orderSn:{}，重试次数:{}", order.getOrderSn(),retryCount,e);
            handleSendFailure(order,topic,retryCount,e);
        }
    }

    /**
     * 处理发送失败，三级重试策略
     * @author wynb
     * @date 2026/1/19 18:05
     */
    private void handleSendFailure(OrderEntity order, String topic, int retryCount, Exception e) {
        //第一层：内存队列快速重试（最多3次）
        if(retryCount < 3){
            RetryTask retryTask = new RetryTask(order,topic,retryCount+1);
            boolean offer = retryQueue.offer(retryTask);
            if(offer){
                log.info("消息进入内存重试队列。orderSn:{}，下次重试次数：{}", order.getOrderSn(),retryCount+1);
            }else{
                //队列满了，降级到数据库
                log.warn("内存重试队列已满，直接降级到数据库。orderSn:{}", order.getOrderSn());
                saveToRetryDatabase(order, topic, retryCount + 1, "队列满了，但是没到最大重试次数");
            }
        }else{
            //超过内存重试次数，持久化到数据库
            saveToRetryDatabase(order, topic, retryCount, "队列满了，并且到最大重试次数了");
        }
    }

    /**
     * 最终兜底：保存到db
     * @author wynb
     * @date 2026/1/19 18:16
     */
    private void saveToRetryDatabase(OrderEntity order, String topic, int retryCount, String failReason) {
        MessageRetryRecord record = new MessageRetryRecord();
        record.setOrderSn(order.getOrderSn());
        record.setRetryCount(retryCount);
        record.setFailReason(failReason);
        record.setTopic(topic);
        record.setContent(JSON.toJSONString(buildMessage(order)));
        record.setStatus(0);    //待重试
        record.setNextRetryTime(new Date((System.currentTimeMillis() + 60000)));    //1min

        retryRecordService.save(record);
        log.info("消息持久化到重试表，等待定时任务处理。orderSn:{}", order.getOrderSn());
    }

    /**
     * 构建消息体
     * @author wynb
     * @date 2026/1/19 17:52
     */
    private OrderMessage buildMessage(OrderEntity order){
        OrderMessage message = new OrderMessage();
        message.setOrderSn(order.getOrderSn());
        message.setUserId(order.getMemberId());
        message.setCouponId(order.getCouponId());
        message.setCreateTime(new Date());
        message.setMessageId(UUID.randomUUID().toString());

        return message;
    }

    /**
     * 内存重试任务封装
     * @author wynb
     * @date 2026/1/19 17:28
     */
    private static class RetryTask {
        final OrderEntity order;
        final String topic;
        final int retryCount;

        RetryTask(OrderEntity order, String topic, int retryCount) {
            this.order = order;
            this.topic = topic;
            this.retryCount = retryCount;
        }
    }
}
