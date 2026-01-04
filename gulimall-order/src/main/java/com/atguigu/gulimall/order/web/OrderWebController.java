package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    OrderService orderService;
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo =  orderService.confirmOrder();
        model.addAttribute("orderConfirmData",confirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     * @author wynb-81
     * @create 2025/6/23
     **/
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
        //下单失败返回到订单确认页重新确认订单信息
        System.out.println("订单提交的数据" + vo);
        if (responseVo.getCode()==0){
            //下单成功，来到支付选择项
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }else {
            String msg = "下单失败：";
            switch (responseVo.getCode()){
                case 1: msg +="订单信息过期，请刷新后再提交"; break;
                case 2: msg +="商品价格发生变化，请确认后再提交"; break;
                case 3: msg +="库存锁定失败，商品库存不足"; break;
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }

}
