 package com.atguigu.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.ware.vo.LockStockResult;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;

import static com.atguigu.common.exception.BizCodeEnume.NO_STOCK_EXCEPTION;


 /**
 * 商品库存
 *
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:40:05
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 锁定库存
     * @author wynb-81
     * @create 2025/6/23
     **/
    @PostMapping("/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo){
        try {
            Boolean stock =  wareSkuService.orderLockStock(vo);
            return R.ok();
        }catch (NoStockException e){
            return R.error(NO_STOCK_EXCEPTION.getCode(),NO_STOCK_EXCEPTION.getMsg());
        }

    }

    /**
     * 查询sku是否有库存
     */
    @PostMapping("/hasstock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds) {
        List<SkuHasStockVo> vos = wareSkuService.getHasStock(skuIds);

        return R.ok().setData(vos);

    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
