package com.atguigu.gulimall.ware.service;

import com.atguigu.gulimall.ware.vo.FareVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:40:05
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVo getFare(Long attrId);
}

