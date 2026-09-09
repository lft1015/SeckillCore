package com.reditickets.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀执行请求数据传输对象（DTO）
 * <p>
 * 封装执行秒杀时提交的参数，使用 Jakarta Validation 进行必填校验
 * </p>
 *
 * @author gugu
 */
@Data
public class SeckillExecuteDTO {

    /** 活动ID，不能为空 */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    /** 用户ID，不能为空 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}