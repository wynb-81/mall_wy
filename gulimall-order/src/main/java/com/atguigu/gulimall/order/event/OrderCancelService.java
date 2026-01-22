package com.atguigu.gulimall.order.event;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.atguigu.gulimall.order.enume.OrderStatusEnum.CANCLED;

@Slf4j
@Service
public class OrderCancelService {
    private final OrderServiceImpl orderService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderCancelService(OrderServiceImpl orderService, OrderEventPublisher orderEventPublisher) {
        this.orderService = orderService;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public void cancelOrder(String orderSn,CancelType cancelType) throws BusinessException {
        OrderEntity order = orderService.getOrderByOrderSn(orderSn);
        if(order == null) {
            throw new BusinessException("订单不存在");
        }

        //更新订单状态
        order.setStatus(CANCLED.getCode());
        order.setModifyTime(new Date());    //取消时间
        orderService.updateById(order);

        //释放库存
        //TODO 也放到消息队列里面，解耦

        //发送取消事件
        orderEventPublisher.publishOrderCancelled(orderSn, order.getMemberId());

        log.info("订单取消成功:orderSn={}",orderSn);
    }

    public enum CancelType {
        USER,
        TIMEOUT
    }
}
