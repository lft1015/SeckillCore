package com.reditickets.seckill.controller;

import com.reditickets.common.result.Result;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.service.SeckillService;
import com.reditickets.seckill.vo.SeckillResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀控制层
 * <p>
 * 提供秒杀执行和秒杀结果查询等 REST API 接口，
 * 是秒杀系统的核心入口，需要配合 Sentinel 进行流量控制
 * </p>
 *
 * @author gugu
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 秒杀执行接口
     * <p>
     * 接收用户秒杀请求，执行 Redis 预扣库存、防重复校验等逻辑，
     * 成功后通过 RocketMQ 异步创建订单，立即返回排队结果
     * </p>
     *
     * @param dto 秒杀请求参数（活动ID、用户ID）
     * @return 秒杀结果（排队中 / 成功 / 失败）
     */
    @PostMapping("/execute")
    public Result<SeckillResultVO> execute(@Valid @RequestBody SeckillExecuteDTO dto) {
        return seckillService.executeSeckill(dto);
    }

    /**
     * 秒杀结果查询接口
     * <p>
     * 根据订单ID查询秒杀最终结果，用于前端轮询获取异步下单结果
     * </p>
     *
     * @param orderId 订单ID
     * @return 秒杀结果视图对象
     */
    @GetMapping("/result/{orderId}")
    public Result<SeckillResultVO> result(@PathVariable Long orderId) {
        return seckillService.getSeckillResult(orderId);
    }
}