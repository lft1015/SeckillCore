package com.reditickets.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 可在此添加自定义 SQL 查询方法
}