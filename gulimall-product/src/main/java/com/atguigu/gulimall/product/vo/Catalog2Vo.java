package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

//二级分类Vo
public class Catalog2Vo {
    private String catalog1Id;  //一级父分类ID
    private List<Catalog3Vo> catalog3List;  //三级子分类
    private String id;
    private String name;


    //三级分类Vo
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catalog3Vo{
        private String catalog2Id;  //父分类，2级分类id
        private String catId;
        private String name;
    }
}
