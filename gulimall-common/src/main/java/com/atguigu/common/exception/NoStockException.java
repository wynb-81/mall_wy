package com.atguigu.common.exception;

public class NoStockException extends RuntimeException{
    private Long skuId;

    public NoStockException(Long skuId, String message) {
        super(message);
        this.skuId = skuId;
    }

    public NoStockException(Long skuId) {
        super("商品库存不足，skuId: " + skuId);
        this.skuId = skuId;
    }

    public Long getSkuId() {
        return skuId;
    }
}
