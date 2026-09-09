package com.reditickets.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillExecuteDTO {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}