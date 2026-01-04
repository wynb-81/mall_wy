package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * 秒杀商品的定时上架
 *  每晚3点上架最近三天需要秒杀的商品
 * @author wynb-81
 * @create 2025/6/25
 **/
@Service
@Slf4j
public class SeckillSkuScheduled {
    @Autowired
    SeckillService seckillService;
    @Autowired
    RedissonClient redissonClient;

    private final String upload_stock = "seckill:upload:lock";

    @Scheduled(cron = "0 * * * * ?")
    public void uploadSeckillSkuLatest3Days(){
        //1.重复上架无需处理
        log.info("上架秒杀的商品信息......");
        //幂等性处理-分布式锁
        RLock lock = redissonClient.getLock(upload_stock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        }finally {
            lock.unlock();
        }
    }
}
