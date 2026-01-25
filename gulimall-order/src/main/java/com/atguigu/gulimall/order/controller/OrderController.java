 package com.atguigu.gulimall.order.controller;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.gulimall.order.event.OrderCancelService;
import com.atguigu.gulimall.order.event.OrderEventPublisher;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * 订单
 *
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:35:27
 */
@RestController
//@RequestMapping("order")
public class OrderController {
    private final OrderEventPublisher orderEventPublisher;
    private final OrderService orderService;
    private final OrderCancelService orderCancelService;

    public OrderController(OrderEventPublisher orderEventPublisher, OrderService orderService, OrderCancelService orderCancelService) {
        this.orderEventPublisher = orderEventPublisher;
        this.orderService = orderService;
        this.orderCancelService = orderCancelService;
    }

    /**
     * 提交订单
     * @author wynb-81
     * @create 2025/6/22
     **/
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo){
        SubmitOrderResponseVo responseVo =  orderService.submitOrder(vo);
        if (responseVo.getCode() ==0){
            //下单成功，跳转到支付页面
            return "pay";
        }else {
            //下单失败，返回到订单确认页重新确认订单信息
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }

    /**
     * 取消订单-用户主动取消
     * @author wynb
     * @date 2026/1/21 17:02
     */
    @PostMapping("/{orderSn}/cancel")
    public R cancelOrder(@PathVariable("orderSn") String orderSn) throws BusinessException {
        orderCancelService.cancelOrder(orderSn,OrderCancelService.CancelType.USER);
        return R.ok("取消成功");
    }

    /**
     * 确认收货
     * @author wynb
     * @date 2026/1/21 17:05
     */
    @PostMapping("/{orderSn}/confirm-receipt")
    public R confirmReceipt(@PathVariable("orderSn") String orderSn){
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);

        //更新订单状态
        order.setStatus(3);
        orderService.updateById(order);

        //发送订单完成事件，核销优惠券
        orderEventPublisher.publishOrderCompleted(orderSn,order.getMemberId());

        return R.ok("确认收货成功");
    }


    /**
     * 返回订单状态
     * @author wynb-81
     * @create 2025/6/24
     **/
    @GetMapping("/status/{orderSn}")
    public R getOrderStatus(@PathVariable("orderSn") String orderSn){
        OrderEntity orderEntity =  orderService.getOrderByOrderSn(orderSn);
        return R.ok().setData(orderEntity);
    }

    /**
     * 查询当前用户所有订单
     * @author wynb-81
     * @create 2025/6/25
     **/
    @PostMapping("/listWithItem")
    public R listWithItem(@RequestBody Map<String, Object> params){
        PageUtils page = orderService.queryPageWithItem(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("order:order:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("order:order:info")
    public R info(@PathVariable("id") Long id){
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("order:order:save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("order:order:update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("order:order:delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
