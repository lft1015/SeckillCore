package com.reditickets.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀日志状态更新 Mapper
 * <p>
 * 订单模块仅用于回写 t_seckill_log 的状态（排队中 → 已下单/已失败），
 * 不负责查询和插入，保持最小权限原则
 * </p>
 *
 * @author gugu
 */
@Mapper
public interface SeckillLogMapper {

    /**
     * 更新秒杀日志为已下单状态
     *
     * @param seckillLogId 秒杀日志ID
     * @return 影响行数
     */
    @Update("UPDATE t_seckill_log SET status = 1, update_time = NOW() WHERE id = #{seckillLogId} AND status = 0")
    int updateToOrdered(@Param("seckillLogId") Long seckillLogId);

    /**
     * 更新秒杀日志为已失败状态
     *
     * @param seckillLogId 秒杀日志ID
     * @param failReason 失败原因
     * @return 影响行数
     */
    @Update("UPDATE t_seckill_log SET status = 2, fail_reason = #{failReason}, update_time = NOW() WHERE id = #{seckillLogId} AND status = 0")
    int updateToFailed(@Param("seckillLogId") Long seckillLogId, @Param("failReason") String failReason);
}