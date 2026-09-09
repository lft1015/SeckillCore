package com.reditickets.seckill.service;

import com.reditickets.common.result.Result;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.vo.SeckillResultVO;

/**
 * 秒杀服务接口
 * <p>
 * 提供秒杀执行和结果查询的业务逻辑定义，
 * 核心流程涉及 Redis 库存预扣、RocketMQ 异步下单等高性能操作
 * </p>
 *
 * @author gugu
 */
public interface SeckillService {

    /**
     * 执行秒杀
     * <p>
     * 校验活动状态 → 防重复校验 → Redis 预扣库存 → 发送异步下单消息 → 返回排队结果
     * </p>
     *
     * @param dto 秒杀请求参数
     * @return 秒杀结果
     */
    Result<SeckillResultVO> executeSeckill(SeckillExecuteDTO dto);

    /**
     * 查询秒杀结果
     * <p>
     * 从 Redis 或数据库查询订单状态，返回秒杀最终结果
     * </p>
     *
     * @param orderId 订单ID
     * @return 秒杀结果视图对象
     */
    Result<SeckillResultVO> getSeckillResult(Long orderId);
}