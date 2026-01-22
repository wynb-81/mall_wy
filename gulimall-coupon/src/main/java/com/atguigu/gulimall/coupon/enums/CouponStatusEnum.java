package com.atguigu.gulimall.coupon.enums;

public enum CouponStatusEnum {
    UNUSED(0),     // 未使用
    LOCKED(1),     // 已锁定（下单但未完成）
    USED(2);       // 已使用（订单完成）

    private Integer code;

    CouponStatusEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }


}
