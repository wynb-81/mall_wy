package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:40:05
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId,@Param("wareId") Long wareId,@Param("skuNum") Integer skuNum);

    Long getSkuStock(Long skuId);

    List<Long> listWareIdHasSkuStock(@Param("skuId") Long skuId);

    Long lockSkuStock(Long skuId, Long wareId, Integer num);

    void unlockStock(@Param("skuId") Long skuId,@Param("wareId") Long wareId,@Param("num") Integer num);
}
