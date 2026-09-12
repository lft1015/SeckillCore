package com.reditickets.seckill.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.seckill.dto.SeckillExecuteDTO;
import com.reditickets.seckill.entity.SeckillLog;
import com.reditickets.seckill.mapper.SeckillLogMapper;
import com.reditickets.seckill.message.SeckillOrderMessage;
import com.reditickets.seckill.service.SeckillService;
import com.reditickets.seckill.vo.SeckillResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现类
 * <p>
 * 实现秒杀执行和结果查询的核心业务逻辑，
 * 涉及 Redis 库存预扣（Lua 脚本原子操作）、RocketMQ 异步下单、防重复秒杀等高性能方案
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final String ACTIVITY_INFO_PREFIX = "activity:info:";
    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String RECORD_PREFIX = "seckill:record:";
    private static final String RESULT_PREFIX = "seckill:result:";
    private static final String MQ_TOPIC = "seckill-order-topic";
    private static final long RESULT_CACHE_TTL_MINUTES = 30;

    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final SeckillLogMapper seckillLogMapper;

    private final RedisScript<Long> deductStockScript;
    private final ObjectMapper objectMapper;

    public SeckillServiceImpl(StringRedisTemplate stringRedisTemplate,
                              RocketMQTemplate rocketMQTemplate,
                              SeckillLogMapper seckillLogMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
        this.seckillLogMapper = seckillLogMapper;

        this.deductStockScript = initDeductStockScript();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    private RedisScript<Long> initDeductStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/seckill_deduct.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public Result<SeckillResultVO> executeSeckill(SeckillExecuteDTO dto) {
        Long activityId = dto.getActivityId();
        Long userId = dto.getUserId();

        // Step 1: 从 Redis 校验活动是否存在及活动时间窗口
        String activityInfoKey = ACTIVITY_INFO_PREFIX + activityId;
        String activityInfoJson = stringRedisTemplate.opsForValue().get(activityInfoKey);
        if (activityInfoJson == null) {
            log.warn("[秒杀] 活动不存在: activityId={}, userId={}", activityId, userId);
            return Result.fail(ResultCode.ACTIVITY_NOT_FOUND);
        }

        Map<String, Object> activityInfo = parseActivityInfo(activityInfoJson);
        LocalDateTime startTime = parseDateTime(activityInfo.get("startTime"));
        LocalDateTime endTime = parseDateTime(activityInfo.get("endTime"));
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startTime)) {
            log.warn("[秒杀] 活动未开始: activityId={}, userId={}, startTime={}", activityId, userId, startTime);
            return Result.fail(ResultCode.ACTIVITY_NOT_STARTED);
        }
        if (now.isAfter(endTime)) {
            log.warn("[秒杀] 活动已结束: activityId={}, userId={}, endTime={}", activityId, userId, endTime);
            return Result.fail(ResultCode.ACTIVITY_ENDED);
        }

        // Step 2: 防重复秒杀校验
        String recordKey = RECORD_PREFIX + activityId + ":" + userId;
        Boolean setSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(recordKey, "1");
        if (Boolean.FALSE.equals(setSuccess)) {
            log.warn("[秒杀] 重复秒杀: activityId={}, userId={}", activityId, userId);
            return Result.fail(ResultCode.SECKILL_REPEATED);
        }

        // Step 3: Redis Lua 原子预扣库存
        String stockKey = STOCK_PREFIX + activityId;
        Long deductResult = stringRedisTemplate.execute(
                deductStockScript,
                Collections.singletonList(stockKey),
                "1");

        if (deductResult == null || deductResult == -1) {
            stringRedisTemplate.delete(recordKey);
            log.warn("[秒杀] 库存缓存不存在: activityId={}, userId={}", activityId, userId);
            return Result.fail(ResultCode.ACTIVITY_NOT_FOUND);
        }
        if (deductResult == 0) {
            stringRedisTemplate.delete(recordKey);
            log.warn("[秒杀] 库存不足: activityId={}, userId={}", activityId, userId);
            return Result.fail(ResultCode.STOCK_NOT_ENOUGH);
        }

        // Step 4: 解析活动信息中的商品ID和秒杀价格
        Long productId = parseLong(activityInfo.get("productId"));
        BigDecimal seckillPrice = parseBigDecimal(activityInfo.get("seckillPrice"));

        // Step 5: 插入秒杀日志到数据库
        SeckillLog seckillLog = new SeckillLog();
        seckillLog.setUserId(userId);
        seckillLog.setProductId(productId);
        seckillLog.setActivityId(activityId);
        seckillLog.setStatus(0);
        seckillLog.setSeckillTime(LocalDateTime.now());

        try {
            seckillLogMapper.insert(seckillLog);
        } catch (Exception e) {
            log.error("[秒杀] DB插入失败，回滚Redis库存: activityId={}, userId={}", activityId, userId, e);
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(recordKey);
            return Result.fail(ResultCode.SECKILL_REPEATED);
        }

        Long seckillLogId = seckillLog.getId();

        // Step 6: 更新防重复标记（写入秒杀日志ID）
        long activityRemainingSeconds = Duration.between(now, endTime).getSeconds();
        if (activityRemainingSeconds > 0) {
            stringRedisTemplate.opsForValue().set(recordKey, String.valueOf(seckillLogId),
                    activityRemainingSeconds, TimeUnit.SECONDS);
        }

        // Step 7: 组装并缓存排队结果
        SeckillResultVO resultVO = new SeckillResultVO();
        resultVO.setSeckillLogId(seckillLogId);
        resultVO.setActivityId(activityId);
        resultVO.setProductId(productId);
        resultVO.setSeckillPrice(seckillPrice);
        resultVO.setStatus(0);
        resultVO.setCreateTime(LocalDateTime.now());

        cacheResult(seckillLogId, resultVO);

        // Step 8: 发送 RocketMQ 异步下单消息
        SeckillOrderMessage message = SeckillOrderMessage.of(
                seckillLogId, userId, activityId, productId, seckillPrice);
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(MQ_TOPIC, message);
            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                log.error("[秒杀] MQ发送状态异常: seckillLogId={}, status={}",
                        seckillLogId, sendResult.getSendStatus());
            }
        } catch (Exception e) {
            log.error("[秒杀] MQ发送失败: seckillLogId={}, activityId={}, userId={}",
                    seckillLogId, activityId, userId, e);
        }

        log.info("[秒杀] 执行成功: seckillLogId={}, activityId={}, userId={}, status=排队中",
                seckillLogId, activityId, userId);
        return Result.success("排队中，请稍后查询结果", resultVO);
    }

    @Override
    public Result<SeckillResultVO> getSeckillResult(Long seckillLogId) {
        // Step 1: 从 Redis 缓存查询
        String resultKey = RESULT_PREFIX + seckillLogId;
        String resultJson = stringRedisTemplate.opsForValue().get(resultKey);
        if (resultJson != null) {
            SeckillResultVO cachedResult = parseResultJson(resultJson);
            if (cachedResult != null && cachedResult.getStatus() != 0) {
                return Result.success(cachedResult);
            }
        }

        // Step 2: 缓存未命中或仍在排队，查询数据库
        SeckillLog seckillLog = seckillLogMapper.selectById(seckillLogId);
        if (seckillLog == null) {
            return Result.fail(ResultCode.SECKILL_FAILED);
        }

        SeckillResultVO resultVO = new SeckillResultVO();
        resultVO.setSeckillLogId(seckillLog.getId());
        resultVO.setActivityId(seckillLog.getActivityId());
        resultVO.setProductId(seckillLog.getProductId());
        resultVO.setStatus(seckillLog.getStatus());
        resultVO.setFailReason(seckillLog.getFailReason());
        resultVO.setCreateTime(seckillLog.getSeckillTime());

        // Step 3: 如果 DB 中已有最终状态（非排队中），回写缓存
        if (seckillLog.getStatus() != null && seckillLog.getStatus() != 0) {
            cacheResult(seckillLogId, resultVO);
        }

        return Result.success(resultVO);
    }

    // ==================== 内部工具方法 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseActivityInfo(String json) {
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (Exception e) {
            log.error("[秒杀] 解析活动缓存失败: json={}", json, e);
            return Collections.emptyMap();
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof String) {
            return LocalDateTime.parse((String) value);
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        return BigDecimal.ZERO;
    }

    private void cacheResult(Long seckillLogId, SeckillResultVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(
                    RESULT_PREFIX + seckillLogId,
                    json,
                    RESULT_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("[秒杀] 结果缓存写入失败: seckillLogId={}", seckillLogId, e);
        }
    }

    private SeckillResultVO parseResultJson(String json) {
        try {
            return objectMapper.readValue(json, SeckillResultVO.class);
        } catch (Exception e) {
            log.warn("[秒杀] 解析结果缓存失败: json={}", json, e);
            return null;
        }
    }
}