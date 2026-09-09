package com.reditickets.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentVO {

    private Long id;
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}