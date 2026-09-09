package com.reditickets.activity.dto;

import lombok.Data;

@Data
public class ActivityListDTO {

    private Integer page = 1;

    private Integer size = 10;

    private Integer status;
}