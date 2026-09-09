package com.reditickets.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {

    private Long id;
    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal seckillPrice;
    private Integer availableStock;
    private Integer totalStock;
    private String imageUrl;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}