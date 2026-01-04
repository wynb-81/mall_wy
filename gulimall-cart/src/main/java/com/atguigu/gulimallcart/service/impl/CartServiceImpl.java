package com.atguigu.gulimallcart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimallcart.feign.ProductFeignService;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.Cart;
import com.atguigu.gulimallcart.vo.CartItem;
import com.atguigu.gulimallcart.vo.SkuInfoVo;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service

public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    private final String CART_PREFIX ="gulimall:cart:";
    /**
     * 给购物车添加数据
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)){
            //2.添加新商品到购物车
            CartItem cartItem = new CartItem();
            //2.1.远程查询当前要添加的商品的信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                //2.2.把上面查询到的商品添加到购物车
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setPrice(data.getPrice());
                cartItem.setTitle(data.getSkuName());
                cartItem.setSkuId(skuId);
            }, executor);

            //2.3.远程查询sku的组合信息，用于在购物车页面展示
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttrValues(values);
            }, executor);

            CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s);    //key为id，值为商品信息

            return cartItem;
        }else{
            //购物车有商品，修改数量就行了
            //将redis中的string类型的数据转回到购物车物品类型，方便修改数量
            CartItem cartItem =  JSON.parseObject(res,CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);
            //再将类型转会去，存到redis中
            cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
            return cartItem;

        }
    }

    /**
     * 获取购物车中的某个购物项
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String srt = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(srt, CartItem.class);
        return cartItem;
    }

    /**
     * 获取整个购物车的信息
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null){
            //1.登录
            String cartKey = CART_PREFIX+userInfoTo.getUserId();
            //2.如果临时购物车的数据还没合并，需要添加到用户购物车中
            String tempCartKey = CART_PREFIX+userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems!=null){
                //需要进行合并
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                //合并之后，清除临时购物车的数据
                clearCart(tempCartKey);
            }
            //3.合并后，再来获取登陆后的购物车
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }else {
            //2.没登陆
            String cartKey = CART_PREFIX+userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    /**
     * 公共方法，清除临时购物车的数据
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public void clearCart(String cartkey){
        redisTemplate.delete(cartkey);

    }

    /**
     * 勾选购物项
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);

    }
    /**
     * 修改购物项数量
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }
    /**
     * 删除购物项
     * @author wynb-81
     * @create 2025/6/8
     **/
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    /**
     * 获取当前用户中购物车勾选的所有商品
     * @author wynb-81
     * @create 2025/6/10
     **/
    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() ==null){
            return null;
        }else{
            String caryKey = CART_PREFIX+userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(caryKey);
            //获取所有被勾选的购物项
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item ->{
                        R price = productFeignService.getPrice(item.getSkuId());
                        //将商品价格更新为最新价格
                        String data =(String) price.get("data");
                        item.setPrice(new BigDecimal(data));
                        return item;
                    })
                    .collect(Collectors.toList());
            return collect;
        }
    }

    /**
     * 公共方法，获取购物车里面的所有购物项
     * @author wynb-81
     * @create 2025/6/8
     **/
    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values!=null && values.size()>0){
            List<CartItem> collect = values.stream().map((obj) -> {
                String str = (String) obj;
                return JSON.parseObject(str, CartItem.class);
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 公共方法，获取要操作的购物车
     * @author wynb-81
     * @create 2025/6/8
     **/
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //判断是否是临时用户登录
        String cartKey = "";
        if (userInfoTo.getUserId() != null){
            //会员登陆
            cartKey = CART_PREFIX+userInfoTo.getUserId();
        }else{
            //临时用户登录
            cartKey = CART_PREFIX+userInfoTo.getUserKey();
        }

        return redisTemplate.boundHashOps(cartKey);
    }
}
