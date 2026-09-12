package com.reditickets.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reditickets.order.dto.CreateOrderDTO;
import com.reditickets.order.entity.Order;
import com.reditickets.order.mapper.SeckillLogMapper;
import com.reditickets.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀下单 RocketMQ 消费者
 * <p>
 * 监听 seckill-order-topic，消费秒杀成功消息后创建订单，
 * 并回写秒杀日志状态和 Redis 结果缓存
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "seckill-order-topic",
        consumerGroup = "seckill-order-consumer-group"
)
public class SeckillOrderConsumer implements RocketMQListener<String> {

    private static final String RESULT_PREFIX = "seckill:result:";
    private static final long RESULT_CACHE_TTL_MINUTES = 30;

    private final OrderService orderService;
    private final SeckillLogMapper seckillLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public SeckillOrderConsumer(OrderService orderService,
                                SeckillLogMapper seckillLogMapper,
                                StringRedisTemplate stringRedisTemplate,
                                RocketMQTemplate rocketMQTemplate) {
        this.orderService = orderService;
        this.seckillLogMapper = seckillLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(String messageJson) {
        log.info("[订单消费者] 收到秒杀消息: {}", messageJson);

        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(messageJson, LinkedHashMap.class);
        } catch (Exception e) {
            log.error("[订单消费者] 消息解析失败: {}", messageJson, e);
            return;
        }

        Long seckillLogId = parseLong(msg.get("seckillLogId"));
        Long userId = parseLong(msg.get("userId"));
        Long activityId = parseLong(msg.get("activityId"));
        Long productId = parseLong(msg.get("productId"));
        BigDecimal seckillPrice = parseBigDecimal(msg.get("seckillPrice"));

        if (seckillLogId == null || userId == null) {
            log.error("[订单消费者] 消息字段缺失: seckillLogId={}, userId={}", seckillLogId, userId);
            return;
        }

        try {
            CreateOrderDTO dto = new CreateOrderDTO();
            dto.setSeckillLogId(seckillLogId);
            dto.setUserId(userId);
            dto.setActivityId(activityId);
            dto.setProductId(productId);
            dto.setSeckillPrice(seckillPrice);
            dto.setQuantity(1);

            Order order = orderService.createOrderInternal(dto);

            seckillLogMapper.updateToOrdered(seckillLogId);

            writeSuccessCache(seckillLogId, order);

            log.info("[订单消费者] 订单创建成功: seckillLogId={}, orderNo={}, userId={}",
                    seckillLogId, order.getOrderNo(), userId);

            sendDelayCancelMessage(order.getId());

        } catch (Exception e) {
            log.error("[订单消费者] 订单创建失败: seckillLogId={}, userId={}", seckillLogId, userId, e);
            seckillLogMapper.updateToFailed(seckillLogId, "订单创建异常: " + e.getMessage());
            writeFailCache(seckillLogId, activityId, productId, "系统异常，秒杀失败");
        }
    }

    private void writeSuccessCache(Long seckillLogId, Order order) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("seckillLogId", seckillLogId);
            result.put("orderId", order.getId());
            result.put("orderNo", order.getOrderNo());
            result.put("activityId", order.getActivityId());
            result.put("productId", order.getProductId());
            result.put("seckillPrice", order.getSeckillPrice());
            result.put("status", 1);
            result.put("createTime", order.getCreateTime() != null
                    ? order.getCreateTime().toString() : LocalDateTime.now().toString());

            stringRedisTemplate.opsForValue().set(
                    RESULT_PREFIX + seckillLogId,
                    objectMapper.writeValueAsString(result),
                    RESULT_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("[订单消费者] 结果缓存写入失败: seckillLogId={}", seckillLogId, e);
        }
    }

    private void writeFailCache(Long seckillLogId, Long activityId, Long productId, String failReason) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("seckillLogId", seckillLogId);
            result.put("activityId", activityId);
            result.put("productId", productId);
            result.put("status", 2);
            result.put("failReason", failReason);
            result.put("createTime", LocalDateTime.now().toString());

            stringRedisTemplate.opsForValue().set(
                    RESULT_PREFIX + seckillLogId,
                    objectMapper.writeValueAsString(result),
                    RESULT_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("[订单消费者] 失败缓存写入失败: seckillLogId={}", seckillLogId, e);
        }
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

    private void sendDelayCancelMessage(Long orderId) {
        try {
            rocketMQTemplate.syncSend("order-delay-topic",
                    MessageBuilder.withPayload(String.valueOf(orderId)).build(),
                    3000, 5);
            log.info("[订单消费者] 延时取消消息已发送: orderId={}", orderId);
        } catch (Exception e) {
            log.error("[订单消费者] 延时取消消息发送失败: orderId={}", orderId, e);
        }
    }
}