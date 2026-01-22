package com.atguigu.gulimall.coupon.service.impl;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.gulimall.coupon.enums.CouponStatusEnum;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.coupon.dao.CouponDao;
import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.atguigu.gulimall.coupon.service.CouponService;
import org.springframework.transaction.annotation.Transactional;


@Service("couponService")
public class CouponServiceImpl extends ServiceImpl<CouponDao, CouponEntity> implements CouponService {

    /**
     * 锁定优惠券——订单创建
     * @author
     * @date 2026/1/22 12:27
     */
    @Transactional
    @Override
    public boolean lockCoupon(Long userId, Long couponId, String orderSn) throws BusinessException{
        CouponEntity coupon = getById(couponId);

        if(coupon.getStatus() != CouponStatusEnum.UNUSED.getCode()){
            throw new BusinessException("优惠券不可用");
        }

        coupon.setStatus(CouponStatusEnum.LOCKED.getCode());
        coupon.setLockOrderSn(orderSn);
        Boolean isLocked = updateById(coupon);
        return isLocked;
    }

    /**
     * 解锁优惠券
     * @author wynb
     * @date 2026/1/22 12:59
     */
    @Override
    public boolean unlockCoupon(Long userId, Long couponId, String orderSn) throws BusinessException {
        CouponEntity coupon = getById(couponId);

        if(coupon.getStatus() != CouponStatusEnum.LOCKED.getCode()){
            throw new BusinessException("优惠券不可用");
        }

        coupon.setStatus(CouponStatusEnum.UNUSED.getCode());
        coupon.setLockOrderSn(orderSn);
        Boolean isLocked = updateById(coupon);
        return isLocked;
    }

    /**
     * 核销优惠券
     * @author wynb
     * @date 2026/1/22 13:01
     */
    @Override
    public boolean completedCoupon(Long userId, Long couponId, String orderSn) throws BusinessException {
        CouponEntity coupon = getById(couponId);

        if(coupon.getStatus() != CouponStatusEnum.LOCKED.getCode()){
            throw new BusinessException("优惠券不可用");
        }

        coupon.setStatus(CouponStatusEnum.USED.getCode());
        coupon.setLockOrderSn(orderSn);
        Boolean isLocked = updateById(coupon);
        return isLocked;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CouponEntity> page = this.page(
                new Query<CouponEntity>().getPage(params),
                new QueryWrapper<CouponEntity>()
        );

        return new PageUtils(page);
    }
}