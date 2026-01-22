package com.atguigu.gulimall.order.config;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;


@Configuration
//@EnableConfigurationProperties(ThreadPoolConfigProperties.class) 、
//在ThreadPoolConfigProperties中已经通过@Componet加入了，再写就注册两次bean了，就会导致pool不知道使用哪个bean
public class MyThreadConfig {
    @Bean(name = "orderThreadPool")
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties pool){
        return new ThreadPoolExecutor(
                pool.getCoreSize(),
                pool.getMaxSize(),
                pool.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(pool.getQueueCapacity()),
                createThreadFactory("gulimall-thread-"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    //TODO 目前返回简单线程创建，后续可以根据业务添加变量，创建细节更多的线程池
//    // 如：创建支付线程工厂（更高优先级）
//    public static ThreadFactory createPaymentThreadFactory() {
//        return new ThreadFactoryBuilder()
//                .setNameFormat("payment-pool-%d")
//                .setDaemon(false)
//                .setPriority(Thread.MAX_PRIORITY)         // 最高优先级
//                .setUncaughtExceptionHandler(createExceptionHandler())
//                .build();
//    }
    private ThreadFactory createThreadFactory(String namePrefix) {
        return new ThreadFactoryBuilder()
                .setNameFormat(namePrefix + "%d")
                .setDaemon(false)
                .build();
    }
}
