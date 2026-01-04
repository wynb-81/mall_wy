package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {
    //收货地址列表
    @Setter @Getter
    List<MemberAddressVo> memberAddressVos;

    //所有选中的购物项
    @Setter @Getter
    List<OrderItemVo> items;

    @Setter @Getter
    Integer integration;    //会员的积分信息==》优惠券信息

    //令牌防重
    @Setter @Getter
    String orderToken;

    @Setter @Getter
    Map<Long,Boolean> stocks;

    public Integer getCount(){
        Integer i = 0;
        if (items!=null){
            for (OrderItemVo item : items) {
               i += item.getCount();
            }
        }
        return i;
    }

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items!=null){
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    //现在没添加优惠，所以直接调用总额的方法就得了
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
