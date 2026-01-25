package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 锁定库存
     * @param skuId 商品ID
     * @param wareId 仓库ID
     * @param num 锁定数量
     * @return 更新影响的行数
     */
    Long lockSkuStock(@Param("skuId") Long skuId,
                      @Param("wareId") Long wareId,
                      @Param("num") Integer num);



    void unlockStock(@Param("skuId") Long skuId,@Param("wareId") Long wareId,@Param("num") Integer num);

}
