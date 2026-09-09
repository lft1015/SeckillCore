package com.reditickets.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 可在此添加自定义 SQL 查询方法
}