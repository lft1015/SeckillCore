package com.reditickets.activity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityVO {

    private Long id;
    private String activityName;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}