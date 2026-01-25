package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.MemberRespVo;
import com.atguigu.common.to.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.event.OrderEventPublisher;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.rocketMQ.OrderMessageService;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static com.atguigu.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;
import static com.atguigu.gulimall.order.enume.OrderStatusEnum.*;


@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private final ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();
    private final MemberFeignService memberFeignService;
    private final CartFeignService cartFeignService;
    private final WmsFeignService wmsFeignService;
    private final ThreadPoolExecutor executor;
    private final StringRedisTemplate redisTemplate;
    private final ProductFeignService productFeignService;
    private final OrderItemService orderItemService;
    private final PaymentInfoService paymentInfoService;
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderMessageService orderMessageService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderServiceImpl(MemberFeignService memberFeignService,
                            CartFeignService cartFeignService,
                            WmsFeignService wmsFeignService,
                            ThreadPoolExecutor executor,
                            StringRedisTemplate redisTemplate,
                            ProductFeignService productFeignService,
                            OrderItemService orderItemService,
                            PaymentInfoService paymentInfoService, RocketMQTemplate rocketMQTemplate, OrderMessageService orderMessageService, OrderEventPublisher orderEventPublisher) {
        this.memberFeignService = memberFeignService;
        this.cartFeignService = cartFeignService;
        this.wmsFeignService = wmsFeignService;
        this.executor = executor;
        this.redisTemplate = redisTemplate;
        this.productFeignService = productFeignService;
        this.orderItemService = orderItemService;
        this.paymentInfoService = paymentInfoService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderMessageService = orderMessageService;
        this.orderEventPublisher = orderEventPublisher;
    }

    /**
     * 提交订单的流程：
     * 1.返回订单确认页需要的数据
     *
     * @author wynb-81
     * @create 2025/6/10
     **/
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
//        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        MemberRespVo memberRespVo = LoginUserInterceptor.getLoginUser();
        //获取请求内容
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //将请求内容塞到每个副线程中，保证其不丢失上下文
            //TODO 线程安全问题，将用户信息直接传递给两个子线程，如果有修改操作，那么设计线程安全问题
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //1.远程查询所有收货地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());

            confirmVo.setMemberAddressVos(address);
        }, executor);

        CompletableFuture<Void> getItemsFuture = CompletableFuture.runAsync(() -> {
            //将请求内容塞到每个副线程中，保证其不丢失上下文
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //2.远程查询购物车所有选中的购物项
            //TODO 现在只实现了从购物车中发起订单请求呗？如果用户直接购物，还没有这个功能
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();

            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream()
                        .collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        });

        //3.查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        //4.防重令牌
        String token = UUID.randomUUID().toString().replace("_", "");
        //给redis中存入，键是前缀＋用户id，值是token.过期时间设置为30分钟
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        //异步任务完成，返回数据
        CompletableFuture.allOf(getAddressFuture, getItemsFuture).get();
        return confirmVo;
    }

    /**
     * 2.提交订单--->指的是用户点击提交订单
     *
     * @author wynb-81
     * @create 2025/6/22
     **/
//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.getLoginUser();
        //1.验证令牌,需要注意令牌的对比和删除操作要保证原子性，否则如果用户点击的很快，还是会重复提交一个订单。所以这个操作用脚本来完成
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then " +    //从redis获取令牌并检查是否存在且匹配
                "return redis.call('del' ,KEYS[1]) " +   //删除令牌并返回1
                "else " +
                "return 0 " +
                "end";
        String orderToken = vo.getOrderToken();
        //原子验证令牌和删除令牌
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()),
                orderToken);

        //验证失败
        if (result == null || result == 0L) {
            response.setCode(1);
            response.setMsg("订单已提交或过期");
            return response;
        }

        //验证成功
        //1.创建订单，订单项等信息
        OrderCreateTo orderCreateTo = createOrder();
        //2.验价
        //TODO 现在这里有空指针异常
//        BigDecimal payAmount = orderCreateTo.getOrder().getPayAmount();
//        BigDecimal payPrice = vo.getPayPrice();
//        if (Math.abs(payAmount.subtract(payPrice).doubleValue()) >= 0.01) {
//            //金额对比失败
//            response.setCode(2);
//            return response;
//        }
        //3.保存订单
        saveOrder(orderCreateTo);
        //4.锁定库存,只要有异常就回滚
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(orderCreateTo.getOrder().getOrderSn());

        List<OrderItemVo> locks = orderCreateTo.getOrderItems().stream().map(item -> {
            OrderItemVo itemVo = new OrderItemVo();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setCount(item.getSkuQuantity());
            itemVo.setTitle(item.getSkuName());
            return itemVo;
        }).collect(Collectors.toList());
        lockVo.setLocks(locks);
        R r = wmsFeignService.orderLockStock(lockVo);   //调用仓库服务，锁定库存
        OrderEntity order = orderCreateTo.getOrder();
        if (r.getCode() == 0) {
            //锁定库存成功
            response.setCode(0);
            response.setMsg("下单成功");
            response.setOrder(order);
            //异步发送MQ消息，保证数据一致性问题以及消息不丢失
            //修改了消息发送方式
            //orderMessageService.sendMessageAfterCommit(orderCreateTo.getOrder(),"ORDER_CREATED_TOPIC");
            orderEventPublisher.publishOrderCreated(order.getOrderSn(), order.getMemberId(), order.getCouponId());
            log.info("发送了MQ消息");
            return response;
        } else {
            //库存锁定失败
            //TODO 这里需要分析具体的原因，是远程服务挂了，导致没锁库存成功；还是说没有库存了导致的？
            return response;
        }
    }

    /**
     * 2.1 创建订单--->或者可以说是生成订单
     *
     * @author wynb-81
     * @create 2025/6/22
     **/
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        //1.生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity orderEntity = buildOrder(orderSn);

        //2.获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3.计算价格相关
        computePrice(orderEntity, itemEntities);
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(itemEntities);

        return createTo;
    }

    /**
     * 2.1.1构建订单的信息
     *
     * @author wynb-81
     * @create 2025/6/22
     **/
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.getLoginUser();

        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(respVo.getId());
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        //获取收货地址信息
        //TODO 当前wms中还没有fare这个方法
//        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
//        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
//        });
//        //设置运费信息
//        entity.setFreightAmount(fareResp.getFare());
//        //设置收货人信息
//        entity.setReceiverCity(fareResp.getAddress().getCity());
//        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
//        entity.setReceiverName(fareResp.getAddress().getName());
//        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
//        entity.setReceiverPhone(fareResp.getAddress().getPhone());
//        entity.setReceiverProvince(fareResp.getAddress().getProvince());
//        entity.setReceiverRegion(fareResp.getAddress().getRegion());
//        //设置订单状态信息
//        entity.setStatus(UNPAID.getCode());
//        entity.setAutoConfirmDay(7);    //自动收货时间

        return entity;
    }

    /**
     * 2.1.2 构建订单信息需要的所有订单项数据
     *
     * @author wynb-81
     * @create 2025/6/22
     **/
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItems -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItems);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 2.1.2.1 构建某一个订单项的具体数据数据
     *
     * @author wynb-81
     * @create 2025/6/22
     **/
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //1.spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());


        //2.sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttrValues(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        //3.积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //4.订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单的实际金额
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = origin.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);

        return itemEntity;
    }

    /**
     * 2.2创建订单--->验价
     *
     * @author wynb-81
     * @create 2025/6/22
     **/
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        //订单的总额，叠加每一个订单项的总额信息
        for (OrderItemEntity entity : itemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth()));
        }
        //1.订单价格相关
        orderEntity.setTotalAmount(total);
        //应付总额，商品价格+运费
        //TODO 此处有空指针异常
        orderEntity.setPayAmount(total);
//        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);
        //设置积分和成长值
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);
    }

    /**
     * 3.保存订单数据
     *
     * @author wynb-81
     * @create 2025/6/23
     **/
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }

    /**
     * 4.锁定库存，远程调用库存服务
     * @author wynb-81
     * @create 2026/1/7
     **/

    /**
     * 5.锁定库存成功后--->获取当前订单的支付信息
     *
     * @author wynb-81
     * @create 2025/6/25
     **/
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(order.getOrderSn());
        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        payVo.setSubject(entity.getSpuName());
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }

    /**
     * 6.处理支付宝的支付结果
     *
     * @author wynb-81
     * @create 2025/6/25
     **/
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //1.保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(infoEntity);

        //2.修改订单状态信息
        if (vo.getTrade_status().equals("TRDAE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            //支付成功状态
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }



    /**
     * 查询当前登录用户的所有订单
     *
     * @author wynb-81
     * @create 2025/6/25
     **/
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.getLoginUser();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );

        List<OrderEntity> order_sn = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(
                    new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(order_sn);

        return new PageUtils(page);
    }

    /**
     * 返回订单状态
     *
     * @author wynb-81
     * @create 2025/6/24
     **/
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));    //根据订单号查询订单详情
    }

    /**
     * 创建秒杀单的详细信息
     *
     * @author wynb-81
     * @create 2025/6/26
     **/
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        //保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());
        orderEntity.setStatus(UNPAID.getCode());
        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(seckillOrder.getOrderSn());
        itemEntity.setRealAmount(multiply);
        itemEntity.setSkuQuantity(seckillOrder.getNum());
        orderItemService.save(itemEntity);

    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

}