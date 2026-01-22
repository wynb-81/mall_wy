package com.atguigu.gulimall.order.rocketMQ;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageRetryRecordDao extends BaseMapper<MessageRetryRecord> {
    // 查询待重试的消息（下次重试时间已到）
    @Select("SELECT * FROM msg_retry_record WHERE status = 0 AND next_retry_time <= NOW() ORDER BY id ASC LIMIT 100")
    List<MessageRetryRecord> selectPendingRecords();
}
