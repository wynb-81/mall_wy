package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVo {
    private Long purchaseId; //整单ID
    private List<Long> items;   //合并项集合
}
