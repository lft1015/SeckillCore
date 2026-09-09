package com.reditickets.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀结果视图对象（VO）
 * <p>
 * 用于接口返回秒杀执行结果，包含订单号、秒杀价格、状态等信息
 * </p>
 *
 * @author gugu
 */
@Data
public class SeckillResultVO {

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private Long productId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 秒杀状态：0=排队中，1=秒杀成功，2=秒杀失败 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}