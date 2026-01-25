package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.MemberRespVo;
import com.atguigu.common.to.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


@Service
public class SeckillServiceImpl implements SeckillService {
    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;

    /**
     * 将秒杀商品添加到购物车
     * @author wynb-81
     * @create 2025/6/26
     **/
    //TODO 我在网上看过说if最好不要超过三层，这个已经好多层了。到时候看看为啥不能超过三层，并且学一下怎么优化
    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        //1.获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (StringUtils.hasText(json)){
            SecKillSkuRedisTo redis = JSON.parseObject(json, SecKillSkuRedisTo.class);
            //校验合法性
            Long startTime = redis.getStartTime();
            Long endTime = redis.getEndTime();
            long time = new Date().getTime();
            long ttl = endTime-time;
            //1.校验时间合法性
            if (time >=startTime&&time<=endTime){
                //2.校验随机码和商品id
                String randomCode = redis.getRandomCode();
                String skuId =  redis.getPromotionSessionId()+"_"+redis.getSkuId();
                if (randomCode.equals(key)&& skuId.equals(killId)){
                    //3.验证购物数量是否合理
                    if (num<=redis.getSeckillLimit()){
                        //4.验证这个人是否已经买过,只要秒杀成功，就去占位
                        String redisKey = respVo.getId()+"_"+skuId;
                        //设置自动过期时间

                        Boolean aBoolean = stringRedisTemplate.opsForValue()
                                .setIfAbsent(redisKey, num.toString(), ttl, MILLISECONDS);
                        if (aBoolean){
                            //占位成功，说明没买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                                boolean b = semaphore.tryAcquire(num);
                                if (b){
                                    //秒杀成功，快速下单
                                    String timeId = IdWorker.getTimeId();
                                    SeckillOrderTo orderTo = new SeckillOrderTo();
                                    orderTo.setOrderSn(timeId);
                                    orderTo.setNum(num);
                                    orderTo.setSeckillPrice(redis.getSeckillPrice());
                                    orderTo.setSkuId(redis.getSkuId());
                                    orderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                    orderTo.setMemberId(respVo.getId());

                                    return timeId;
                                }
                               return null;
                        }else{
                            return null;
                        }

                    }
                }
            }else{
                return null;
            }
        }else{
            return null;
        }
        return null;
    }

    /**
     * 返回当前时间可以参与秒杀的商品
     * @author wynb-81
     * @create 2025/6/26
     **/
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        //1.确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSION_CACHE_PREFIX, ""); //将前缀去掉
            //将前后时间节点分开存储为开始和结束
            String[] s = replace.split("_");
            Long start = Long.parseLong(s[0]);
            Long end = Long.parseLong(s[1]);
            //判断当前时间是否处于这个时间段
            if (time>=start && time<=end){
                //2.获取这个场次需要的所有商品信息
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SESSION_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    List<SecKillSkuRedisTo> collect = list.stream().map(item -> {
                        SecKillSkuRedisTo redis =  JSON.parseObject((String) item,SecKillSkuRedisTo.class);
                        return redis;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }


        return Collections.emptyList();
    }

    /**
     * 查询当前商品是否参与秒杀
     * @author wynb-81
     * @create 2025/6/26
     **/
    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys!=null && keys.size()>0){
            String regx = "\\d_"+skuId;
            for (String key : keys) {
                if (Pattern.matches(regx,key)){
                    String json = hashOps.get(key);
                    SecKillSkuRedisTo redisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
                    long current = new Date().getTime();
                    if (!(current>=redisTo.getStartTime() && current<=redisTo.getEndTime())){
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }
            }
        }
        return null;
    }

    /**
     * 上架三日内参加秒杀的商品
     * @author wynb-81
     * @create 2025/6/25
     **/
    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.查找需要参与秒杀的活动
        R session = couponFeignService.getLatest3DaySession();
        if (session.getCode()==0){
            //上架商品
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis 1.活动信息 2.活动的关联商品信息
            //1.活动信息
            saveSessionInfos(sessionData);
            //2.活动的关联商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session->{
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getStartTime().getTime();
            String key =  SESSION_CACHE_PREFIX+startTime+"_"+endTime;
            Boolean b = stringRedisTemplate.hasKey(key);
            if (!b){
                List<String> collect = session.getRelationSkus().stream().map(item ->item.getPromotionSessionId()+"_"+ item.getSkuId().toString()).collect(Collectors.toList());//商品id
                stringRedisTemplate.opsForList().leftPushAll(key,collect);
            }


        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session->{
            BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                //4.商品的随机码：
                String token = UUID.randomUUID().toString().replace("-", "");
                if (!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString()+"_"+ seckillSkuVo.getSkuId().toString())){
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    //缓存商品
                    //1.sku的基本数据
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (skuInfo.getCode()==0){
                        SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }

                    //2.sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);

                    //3.当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());


                    redisTo.setRandomCode(token);
                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId().toString()+"_"+ seckillSkuVo.getSkuId().toString(),jsonString);
                    //如果当前这个场次的商品的库存信息已经上架，就不需要上架了
                    //通过信号量来限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());    //商品可以秒杀的数量作为信号量
                }



            });

        });
    }

}
