package com.atguigu.gulimall.order.rocketMQ;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("msg_retry_record")
public class MessageRetryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSn;
    private String topic;
    private String content; // 消息内容JSON
    private Integer retryCount;
    private Integer status; // 0-待处理, 1-处理成功, 2-最终失败
    private String failReason;
    private Date nextRetryTime;
    private Date createTime;
    private Date updateTime;
}
