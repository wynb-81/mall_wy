package com.atguigu.gulimall.search.controller;

import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.atguigu.common.exception.BizCodeEnume.PRODUCT_UP_EXCEPTION;

@Slf4j
@RestController
@RequestMapping("/search/save")
public class ElasticSaveController {
    @Autowired
    ProductSaveService productSaveService;

    //上架商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels){
        boolean b= false;
        try {
            b = productSaveService.productStatusUp(skuEsModels);
        }catch (Exception e){
            log.error("ElasticSaveController商品上架错误：{}",e);
            return R.error(PRODUCT_UP_EXCEPTION.getCode(),PRODUCT_UP_EXCEPTION.getMsg());
        }

        if(!b){
            return R.ok();
        }else {
            return R.error(PRODUCT_UP_EXCEPTION.getCode(),PRODUCT_UP_EXCEPTION.getMsg());
        }
    }
}
