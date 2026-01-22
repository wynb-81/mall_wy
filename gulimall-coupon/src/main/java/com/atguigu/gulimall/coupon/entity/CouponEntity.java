package com.atguigu.gulimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 优惠券信息
 * 
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:18:06
 */
@Data
@TableName("sms_coupon")
public class CouponEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	private String LockOrderSn;	//锁定该优惠券的订单号
	private Integer Status;	//状态：0未使用，1锁定，2核销（已使用）

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * 优惠卷类型[0->全场赠券；1->会员赠券；2->购物赠券；3->注册赠券]
	 */
	private Integer couponType;

	/**
	 * 优惠卷名字
	 */
	private String couponName;

	/**
	 * 金额
	 */
	private BigDecimal amount;

	/**
	 * 使用门槛
	 */
	private BigDecimal minPoint;


}
