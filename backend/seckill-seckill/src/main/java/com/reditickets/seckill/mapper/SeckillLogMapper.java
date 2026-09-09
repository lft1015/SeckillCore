package com.reditickets.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reditickets.seckill.entity.SeckillLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀日志数据访问层
 * <p>
 * 继承 MyBatis Plus BaseMapper 接口，自动获得 CRUD 基础操作能力，
 * 用于记录每次秒杀操作的详细日志，可在此接口中添加自定义 SQL 查询方法
 * </p>
 *
 * @author gugu
 */
@Mapper
public interface SeckillLogMapper extends BaseMapper<SeckillLog> {
    // 可在此添加自定义 SQL 查询方法
}