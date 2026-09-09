package com.reditickets.product.dto;

import lombok.Data;

@Data
public class ProductListDTO {

    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private Integer status;
}