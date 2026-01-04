package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
    * 两种请求路径写法
     * 1.product/skuinfo/info/{skuId}
     * 2.api/product/skuinfo/info/{skuId}
     * 第二种写法就把请求发送到网关，上面也改成网关就行了
    * */
    @RequestMapping("product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

}
