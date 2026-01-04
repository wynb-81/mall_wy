package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 * 有三个方法可以进入搜索页面：
 *  1.点击三级分类
 *  2.在搜索栏搜索：keyword->skuTitle
 *  3.复杂筛选后进入，这个最复杂，而且在检索完之后，还可能按照某种规则进行排序并且过滤后返回
 */
@Data
public class SearchParam {
    private String keyword; //传递过来的全文检索参数
    private Long catalog3Id;    //三级分类Id

    /**
     *  3.1排序条件：saleCount（销量）,hotScore（热度评分）,skuPrice（价格）
     */
    private String sort;

    /**
     *  3.2过滤条件：hasStock（是否有货）,skuPrice（价格区间）,
     *             brandId（指定某个品牌,支持多选）,catalog3Id,attrs(属性，每个属性中用：分割)
     */
    private Integer hasStock =1; //默认有库存
    private String skuPrice;
    private List<Long> brandId;
    private List<String> attrs;
    private Integer pageNum =1;    //页码

    private String _queryString;    //原生的所有查询条件

}
