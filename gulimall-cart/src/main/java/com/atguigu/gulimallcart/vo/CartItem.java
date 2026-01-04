package com.atguigu.gulimallcart.vo;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车里面的每一个购物项
 * @author wynb-81
 * @create 2025/6/6
 **/
public class CartItem {
    private Long skuId;
    private Boolean check = true;   //是否已经选中
    private String title;
    private String image;
    private List<String> skuAttrValues;   //属性
    private BigDecimal price;   //单价
    private Integer count;  //选购的数量
    private BigDecimal totalPrice;  //总价应该根据数量来动态计算


    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Boolean getCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSkuAttrValues() {
        return skuAttrValues;
    }

    public void setSkuAttrValues(List<String> skuAttrValues) {
        this.skuAttrValues = skuAttrValues;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(""+this.count));
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }




}
