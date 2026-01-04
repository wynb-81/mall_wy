package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //1.sku基本信息获取
    private SkuInfoEntity info;

    private boolean hasStock = true;
    //2.sku图片信息获取
    private List<SkuImagesEntity> images;
    //3.获取spu的销售属性组合
    private List<SkuItemSaleAttrVo> saleAttr;
    //4.获取spu的介绍
    private SpuInfoDescEntity desc;
    //5.获取spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;

    seckillSkuVo seckillInfoVo; //当前商品的秒杀优惠信息

}
