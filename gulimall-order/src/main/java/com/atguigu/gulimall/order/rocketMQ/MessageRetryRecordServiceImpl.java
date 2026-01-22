package com.atguigu.gulimall.order.rocketMQ;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class MessageRetryRecordServiceImpl extends ServiceImpl<MessageRetryRecordDao, MessageRetryRecord> implements MessageRetryRecordService {
    private final MessageRetryRecordDao messageRetryRecordDao;
    private final RocketMQTemplate rocketMQTemplate;

    public MessageRetryRecordServiceImpl(MessageRetryRecordDao messageRetryRecordDao, RocketMQTemplate rocketMQTemplate) {
        this.messageRetryRecordDao = messageRetryRecordDao;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 定时任务：扫描并重试失败的消息
     * @author wynb
     * @date 2026/1/19 18:33
     */
    @Scheduled(fixedDelay = 60000) // 每60秒执行一次
    public void retryFailedMessages(){
        List<MessageRetryRecord> records = messageRetryRecordDao.selectPendingRecords();

        for(MessageRetryRecord record : records){
            try{
                //构建消息重新发送
                Message<String> message = MessageBuilder
                        .withPayload(record.getContent())
                        .setHeader(RocketMQHeaders.KEYS,record.getOrderSn())
                        .build();

                SendResult result = rocketMQTemplate.syncSend(record.getTopic(), message);

                //发送成功，更新状态
                record.setStatus(1);
                record.setUpdateTime(new Date());
                messageRetryRecordDao.updateById(record);
                log.info("定时任务重试消息成功。orderSn:{}, msgId:{}", record.getOrderSn(), result.getMsgId());
            }catch (Exception e){
                // 重试失败，更新重试次数和时间
                record.setRetryCount(record.getRetryCount() + 1);
                record.setFailReason(e.getMessage());

                // 设置下次重试时间（指数退避）
                long delay = (long) Math.min(300, Math.pow(2, record.getRetryCount())) * 60000; // 最大5小时
                record.setNextRetryTime(new Date(System.currentTimeMillis() + delay));

                // 如果重试超过10次，标记为最终失败
                if (record.getRetryCount() >= 10) {
                    record.setStatus(2);
                    log.error("消息达到最大重试次数，标记为最终失败。orderSn:{}", record.getOrderSn());
                }

                messageRetryRecordDao.updateById(record);
                log.warn("定时任务重试消息失败。orderSn:{}, 重试次数:{}", record.getOrderSn(), record.getRetryCount(), e);
            }
        }
    }

}
