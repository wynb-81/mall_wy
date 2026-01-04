package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundsTo;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;

import static com.atguigu.common.constant.ProductConstant.StatusEnum.SPU_UP;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SpuImagesService spuImagesService;
    @Autowired
    AttrService attrService;
    @Autowired
    ProductAttrValueService productAttrValueService;
    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        //1、保存spu基本信息:pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        //2、保存Spu的描述图片:pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", decript));    //保存描述信息
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        //3、保存spu的图片集:pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(),images);

        //4、保存spu的规格参数:pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(item -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(item.getAttrId());
            AttrEntity attrEntity = attrService.getById(item.getAttrId());
            valueEntity.setAttrName(attrEntity.getAttrName());
            valueEntity.setAttrValue(item.getAttrValues());
            valueEntity.setQuickShow(item.getShowDesc());
            valueEntity.setSpuId(item.getAttrId());

            return valueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        //5、保存spu的积分信息:sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(bounds, spuBoundsTo);
        spuBoundsTo.setSpuId(infoEntity.getId());
        R r1 =  couponFeignService.saveSpuBounds(spuBoundsTo);
        if (r1.getCode() !=0){
            log.error("远程保存spu积分信息失败");
        }



        //6、保存当前spu对应的所有sku信息
        //6.1）、sku的基本信息:pms_sku_info
        List<Skus> skus = vo.getSkus();
        if(skus != null && skus.size() > 0) {
            skus.forEach(item->{
                //寻找默认图片地址
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if(image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());  //品牌ID
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());  //三级分类ID
                skuInfoEntity.setSaleCount(0L); //刚上架，销量默认是0
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();  //自增ID

                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    //返回true就是需要，false的图片就会被剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                //6.2）、sku的图片信息:pms_sku_images
                //TODO 没有图片的，路径无需保存
                skuImagesService.saveBatch(imagesEntities);

                //6.3）、sku的销售属性信息:pms_sku_sale_attr_vaLue
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);
                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                //6.4）、sku的优惠、满减等信息:gulimall_sms->sms_sku_ladder\sms_sku_full_reduction\sms
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId); //除了skuId都可以复制，但是Id没有，就得额外获取
                if (skuReductionTo.getFullCount() >0 || skuReductionTo.getFuliPrice().compareTo(new BigDecimal("0")) > 0){
                    R r2 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r2.getCode() != 0){
                        log.error("远程保存sku优惠信息失败");
                    }
                }

            });
        }
    }

    /**
     *  保存spu基本信息
     */
    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    /**
     * SPU检索
     * */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        //模糊检索条件
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("id", key).or().like("spu_name", key);
            });
        }
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status", status);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equals(brandId)){
            wrapper.eq("brand_id", brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equals(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    /**
     * 商品上架功能
     * */
    @Override
    public void up(Long spuId) {
        //1.查出当前spuid对应的所有sku信息
        List<SkuInfoEntity> skus =  skuInfoService.getSkuBuSpuId(spuId);

        //TODO 4、查询当前sku的所有可以被用来检索的规格属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrlistforspu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());

        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        //转换为Set集合,转换为set集合的目的：以空间换时间，将查找的时间复杂度从O(N)降到O(1)
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item -> idSet.contains(item.getAttrId())
        ).map(item -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        //TODO 1、发送远程调用，库存系统查询是否有库存 hasStock
        List<Long> skuIdList =
                skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        try {
            R skuHasStock = wareFeignService.getSkuHasStock(skuIdList);
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {};
            stockMap = skuHasStock.getData(typeReference).stream()
                    .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
        }catch (Exception e){
            log.error("库存服务查询异常：原因{}",e);
        }
        //2.封装每个sku的信息，封装成SkuEsModel
        Map<Long, Boolean> finalStockMap = stockMap;

        //拼装最后要上架的产品的属性
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            //组装需要的数据
            SkuEsModel esModel = new SkuEsModel();
            //2.1拷贝共同的数据
            BeanUtils.copyProperties(sku, esModel);
            //2.2单独处理SkuEsModel和SkuEntity中不一样的数据
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            //2.3设置库存信息
            if(finalStockMap ==null){
                esModel.setHasStock(true);
            }else{
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
//            esModel.setHasStock(finalStockMap.get(sku.getSkuId()));

            //TODO 2、热度评分，设置为0.自己可以扩展这个功能   hotScore
            esModel.setHotScore(0L);

            //TODO 3、查询品牌和分类的名字信息
            BrandEntity brand = brandService.getById(sku.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            //2.4设置检索属性
            esModel.setAttrs(attrsList);

            CategoryEntity category = categoryService.getById(sku.getCatalogId());
            esModel.setCatalogName(category.getName());
            return esModel;
        }).collect(Collectors.toList());

        //TODO 5、 将数据发送给es进行保存，search模块的服务
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() ==0){
            //远程调用成功
            //TODO 6、修改当前SPU的上架状态
            baseMapper.updateSpuStatus(spuId,SPU_UP.getCode());
        }else{
            //远程调用失败
        }

    }

    /**
     * 根据skuid查询spu信息
     * @author wynb-81
     * @create 2025/6/22
     **/
    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long id) {
        SkuInfoEntity info = skuInfoService.getById(id);
        Long spuId = info.getSpuId();
        SpuInfoEntity spuInfoEntity = getById(spuId);
        return spuInfoEntity;
    }
}