package com.atguigu.gulimall.seckill.to;

import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecKillSkuRedisTo {
    private Long id;

    private Long promotionId;

    private Long promotionSessionId;
    private String randomCode;  //随机码

    private Long skuId;

    private BigDecimal seckillPrice;

    private Integer  seckillCount;

    private Integer seckillLimit;

    private Integer seckillSort;

    //sku的详细信息
    private SkuInfoVo skuInfo;

    private Long startTime;
    private Long endTime;
}
