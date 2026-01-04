package com.atguigu.gulimallcart.controller;

import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.Cart;
import com.atguigu.gulimallcart.vo.CartItem;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;


@Controller
public class CartController {
    @Autowired
    CartService cartService;

    /**
     * 获取当前用户中购物车勾选的所有商品
     * @author wynb-81
     * @create 2025/6/10
     **/
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }

    /**
     * 删除购物项
     * @author wynb-81
     * @create 2025/6/8
     **/
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html?skuid="+skuId;

    }

    /**
     * 改变商品的数量
     * @author wynb-81
     * @create 2025/6/8
     **/
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html?skuid="+skuId;

    }

    /**
     * 勾选某一项
     * @author wynb-81
     * @create 2025/6/8
     **/
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("checked") Integer check){
        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html?skuid="+skuId;

    }

    /**
     * 查看购物车
     * @author wynb-81
     * @create 2025/6/8
     **/
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        //快速获取拦截器里面的用户信息
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @Param  添加哪个商品？添加几件？
     * @author wynb-81
     * @create 2025/6/6
     **/
    @GetMapping("/addCartItem")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes ra) throws ExecutionException, InterruptedException {
        //快速获取拦截器里面的用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        cartService.addToCart(skuId,num);
//        model.addAttribute("skuId",skuId);        //要注意括号内的名字是和前端遍历绑定的
        ra.addAttribute("skuId",skuId);

        return "redirect:http://cart.gulimall.com/addToCartSuccess.html?skuid="+skuId;
    }

    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuid") Long skuId, Model model){
        //重定向到成功页面，再次查询购物车数据即可
        CartItem item =  cartService.getCartItem(skuId);
        model.addAttribute("item",item);
        return "redirect:http://cart.gulimall.com/cart.html";
    }
}
