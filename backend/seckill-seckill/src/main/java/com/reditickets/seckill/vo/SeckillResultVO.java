package com.reditickets.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillResultVO {

    private Long orderId;
    private String orderNo;
    private Long activityId;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer status;
    private LocalDateTime createTime;
}