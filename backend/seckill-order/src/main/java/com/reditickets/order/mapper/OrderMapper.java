package com.reditickets.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 可在此添加自定义 SQL 查询方法
}