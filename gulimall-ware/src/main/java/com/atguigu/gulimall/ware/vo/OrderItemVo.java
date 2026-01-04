package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {
    private Long skuId;
    private String title;
    private String image;
    private List<String> skuAttrValues;   //属性
    private BigDecimal price;   //单价
    private Integer count;  //选购的数量
    private BigDecimal totalPrice;  //总价应该根据数量来动态计算
//    private Boolean hasStock;
    private BigDecimal weight;
}
