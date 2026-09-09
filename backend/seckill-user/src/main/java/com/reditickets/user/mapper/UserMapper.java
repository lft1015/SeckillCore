package com.reditickets.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 * <p>
 * 继承 MyBatis Plus BaseMapper 接口，自动获得 CRUD 基础操作能力，
 * 可在此接口中添加自定义 SQL 查询方法
 * </p>
 *
 * @author gugu
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 可在此添加自定义 SQL 查询方法
}