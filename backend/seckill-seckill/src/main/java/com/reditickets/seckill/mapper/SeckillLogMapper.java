package com.reditickets.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.seckill.entity.SeckillLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeckillLogMapper extends BaseMapper<SeckillLog> {
    // 可在此添加自定义 SQL 查询方法
}