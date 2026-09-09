package com.reditickets.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求数据传输对象（DTO）
 * <p>
 * 封装秒杀成功后创建订单所需的参数，包含用户、活动、商品、秒杀价格等信息
 * </p>
 *
 * @author gugu
 */
@Data
public class CreateOrderDTO {

    /** 用户ID */
    private Long userId;

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private Long productId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 购买数量 */
    private Integer quantity;
}