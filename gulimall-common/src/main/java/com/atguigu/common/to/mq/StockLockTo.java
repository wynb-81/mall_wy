package com.atguigu.common.to.mq;

import lombok.Data;

import java.util.List;

@Data
public class StockLockTo {
    private Long id;//库存工作单id
    private StockDetailTo detail; //工作详情的所有id
}
