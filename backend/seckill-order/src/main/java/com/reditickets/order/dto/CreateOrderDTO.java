package com.reditickets.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 秒杀价格 */
    @NotNull(message = "秒杀价格不能为空")
    @Positive(message = "秒杀价格必须大于0")
    private BigDecimal seckillPrice;

    /** 购买数量 */
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity = 1;
}