package com.atguigu.gulimall.ware.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLockResultMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;
    private String orderSn;
    private Boolean success;
    private String errorMsg;
    private List<LockDetail> lockDetails;
    private Date processTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LockDetail implements Serializable {
        private Long skuId;
        private Long wareId;
        private Integer lockedNum;
        private Boolean success;
    }
}
