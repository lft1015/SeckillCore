package com.reditickets.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.activity.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    // 可在此添加自定义 SQL 查询方法
}