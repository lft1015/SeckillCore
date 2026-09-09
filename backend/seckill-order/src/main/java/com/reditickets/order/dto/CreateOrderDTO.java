package com.reditickets.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderDTO {

    private Long userId;

    private Long activityId;

    private Long productId;

    private BigDecimal seckillPrice;

    private Integer quantity;
}