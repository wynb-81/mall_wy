package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class seckillSkuVo {
    private Long id;

    private Long promotionId;

    private Long promotionSessionId;
    private String randomCode;  //随机码

    private Long skuId;

    private BigDecimal seckillPrice;

    private BigDecimal seckillCount;

    private BigDecimal seckillLimit;

    private Integer seckillSort;



    private Long startTime;
    private Long endTime;
}
