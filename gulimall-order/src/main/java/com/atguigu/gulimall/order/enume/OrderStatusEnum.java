package com.atguigu.gulimall.order.enume;

/**
 * 订单状态枚举
 * @author Jerry
 */

public enum OrderStatusEnum {
    UNPAID(0,"待付款"),
    PAYED(1,"已付款"),
    RECIEVED(2,"已完成"),
    CANCLED(3,"已取消"),
    SENDED(4,"已发货"),
    SERVICING(5,"售后中"),
    SERVICED(6,"售后完成");

    private Integer code;
    private String msg;

    OrderStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
