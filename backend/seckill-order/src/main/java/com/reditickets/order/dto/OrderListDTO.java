package com.reditickets.order.dto;

import lombok.Data;

@Data
public class OrderListDTO {

    private Integer page = 1;

    private Integer size = 10;

    private Long userId;
}