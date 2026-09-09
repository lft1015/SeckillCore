package com.reditickets.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.order.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
    // 可在此添加自定义 SQL 查询方法
}