package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {
    //封装订单提交的数据
    private Long addrId;
    private Integer payType;
    //仿照JD，无需提交需要购买的商品，再去购物车获取一遍勾选的商品就可以了
    private String orderToken;
    private BigDecimal payPrice;
    private String note; //订单备注
    //用户相关信息从session中获取
}
