package com.reditickets.seckill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.service.SeckillService;
import com.reditickets.seckill.vo.SeckillResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 是秒杀系统的核心入口，配合 Sentinel 进行流量控制和熔断降级
 * </p>
 *
 * @author gugu
 */
@Slf4j
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
    @SentinelResource(value = "seckillExecute", blockHandler = "handleExecuteBlock")
    public Result<SeckillResultVO> execute(@Valid @RequestBody SeckillExecuteDTO dto) {
        return seckillService.executeSeckill(dto);
    }

    /**
     * 秒杀结果查询接口
     * <p>
     * 根据秒杀日志ID查询秒杀最终结果，用于前端轮询获取异步下单结果
     * </p>
     *
     * @param seckillLogId 秒杀日志ID
     * @return 秒杀结果视图对象
     */
    @GetMapping("/result/{seckillLogId}")
    @SentinelResource(value = "seckillResult", blockHandler = "handleResultBlock")
    public Result<SeckillResultVO> result(@PathVariable Long seckillLogId) {
        return seckillService.getSeckillResult(seckillLogId);
    }

    /**
     * 秒杀执行限流降级处理
     */
    public Result<SeckillResultVO> handleExecuteBlock(SeckillExecuteDTO dto, BlockException e) {
        log.warn("[秒杀] 接口限流: activityId={}, userId={}", dto.getActivityId(), dto.getUserId());
        return Result.fail(ResultCode.SECKILL_FAILED.getCode(), "活动太火爆了，请稍后再试");
    }

    /**
     * 结果查询限流降级处理
     */
    public Result<SeckillResultVO> handleResultBlock(Long seckillLogId, BlockException e) {
        log.warn("[秒杀] 查询限流: seckillLogId={}", seckillLogId);
        return Result.fail(ResultCode.SECKILL_FAILED.getCode(), "查询过于频繁，请稍后再试");
    }
}