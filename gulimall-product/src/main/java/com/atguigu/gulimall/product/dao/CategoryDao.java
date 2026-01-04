package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 11:20:11
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
