package com.reditickets.seckill.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RocketMQ 秒杀下单消息体
 * <p>
 * 秒杀成功后投递到 seckill-order-topic，由 Order 服务消费创建订单。
 * 消息体包含 seckillLogId 用于 Order 侧幂等防重复消费。
 * </p>
 *
 * @author gugu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long seckillLogId;
    private Long userId;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer quantity;
    private LocalDateTime createTime;

    public static SeckillOrderMessage of(Long seckillLogId, Long userId, Long activityId,
                                          Long productId, BigDecimal seckillPrice) {
        SeckillOrderMessage msg = new SeckillOrderMessage();
        msg.setSeckillLogId(seckillLogId);
        msg.setUserId(userId);
        msg.setActivityId(activityId);
        msg.setProductId(productId);
        msg.setSeckillPrice(seckillPrice);
        msg.setQuantity(1);
        msg.setCreateTime(LocalDateTime.now());
        return msg;
    }
}