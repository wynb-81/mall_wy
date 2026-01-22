package com.atguigu.gulimall.coupon.service;

import com.atguigu.common.exception.BusinessException;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.CouponEntity;

import java.util.Map;

/**
 * 优惠券信息
 *
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:18:06
 */
public interface CouponService extends IService<CouponEntity> {

    PageUtils queryPage(Map<String, Object> params);

    boolean lockCoupon(Long userId, Long couponId, String orderSn) throws BusinessException;

    boolean unlockCoupon(Long userId, Long couponId, String orderSn) throws BusinessException;

    boolean completedCoupon(Long userId, Long couponId, String orderSn) throws BusinessException;
}

