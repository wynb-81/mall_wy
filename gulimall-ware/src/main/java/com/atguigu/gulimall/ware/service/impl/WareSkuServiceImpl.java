package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.vo.*;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    WareSkuDao wareSkuDao;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    WareOrderTaskService orderTaskService;
    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    OrderFeignService orderFeignService;



    private void unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId){
        //库存解锁
        wareSkuDao.unlockStock(skuId,wareId,num);
        //更新库存工作单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);    //变为已解锁
        orderTaskDetailService.updateById(entity);
    }

    /**
     * 为某个订单锁定库存
     * 库存解锁的场景：
     * 1）下订单成功，订单过期没有支付被系统自动取消、被用户手动取消，都需要解锁库存
     * 2）下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的库存就要自动解锁
     * @author wynb-81
     * @create 2025/6/23
     **/
//    @Transactional(rollbackFor = NoStockException.class)  默认只要是运行时异常都会回滚
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //保存库存工作单详情
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);

        //1.找到每件商品，在哪个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这件商品在哪里有库存
            List<Long> wareIds =  wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.isEmpty()){
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功返回1，1行记录受到影响。失败返回0
                Long count =  wareSkuDao.lockSkuStock(skuId,wareId,hasStock.getNum());
                if (count == 1){
                    skuStocked = true;
                    //告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity entity =
                            new WareOrderTaskDetailEntity(null, skuId, "",
                                    hasStock.getNum(),
                                    taskEntity.getId(),
                                    wareId, 1);
                    orderTaskDetailService.save(entity);
                    StockLockTo stockLockTo = new StockLockTo();
                    stockLockTo.setId(taskEntity.getId());
//                    stockLockTo.setDetailId(entity.getId());    //每锁定一个库存，就发送一次id
                    //但是只发送id的话不合理，这样会导致如果锁前两个成功了，但是锁第三个商品失败的这中情况发生时，
                    //会导致不知道前面锁了几个，找不到数据，所以要把锁定的详情都发送过去
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(entity,stockDetailTo);
                    stockLockTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.lock",stockLockTo);
                    break;
                }else {
                    //当前仓库锁失败，重试下一个仓库

                }
                if (skuStocked == false){
                    //当前商品所有仓库都没锁住
                    throw new NoStockException(skuId);
                }
            }
        }

        //3.只要没在上面抛出异常，就说明全部锁成功了
        return true;
    }

    /**
     * 解锁库存
     * @author wynb-81
     * @create 2025/6/24
     **/
    @Override
    public void unlockStock(StockLockTo to) {
        System.out.println("收到解锁库存的消息");
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();
       /* 解锁
        1、查询数据库关于这个订单的锁定库存信息
        有:证明库存锁定成功了，是否要解锁，还需要看订单情况：
           1.没有这个订单，必须解锁
           2.没有这个订单，就不是解锁库存了，而是要判断订单的状态：
             a.订单状态已经取消，解锁库存
             b.订单状态没有取消，不需要解锁
        没有：库存锁定失败了，库存回滚了，这种情况无需解锁
        */
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId!=null){
            //解锁
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();   //根据订单号查询订单的状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0 ){
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                if (data==null || data.getStatus() == 4){
                    //订单不存在或者订单已经被取消了，可以解锁库存.并且只有是已锁定状态才可以解锁。已经解锁或者已经扣减的状态就不能解锁了
                    if (byId.getLockStatus() == 1){
                        unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detailId);
                    }
                }
            }
        }
    }

    /**
     * 防止订单服务卡顿，导致订单状态消息一直改不了，库存消息优先到期。
     * 查订单状态新建状态，什么都不做就走了，导致卡顿的订单永远不能解锁库存
     * @author wynb-81
     * @create 2025/6/25
     **/
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下库存解锁状态，防止重复解锁库存
        WareOrderTaskEntity task =  orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();   //根据库存工作单的id找到所有未解锁商品，状态为1的商品
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id)
                .eq("lock_status", 1));

        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }

    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),queryWrapper

        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断如果还灭有这个库存记录，就是新增操作
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //添加冗余字段，如果失败，事务无需回滚
            try{
                R info = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName(data.get("skuName").toString());
                }
            }catch (Exception e){

            }


            wareSkuDao.insert(wareSkuEntity);
        }else{
            wareSkuDao.addStock(skuId,wareId,skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //查询当前的库存总量
            Long count  = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ?false: count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

}