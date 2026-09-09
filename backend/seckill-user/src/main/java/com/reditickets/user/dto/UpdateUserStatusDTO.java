package com.reditickets.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户状态请求数据传输对象（DTO）
 * <p>
 * 供管理员冻结/解冻用户，status: 0=冻结 / 1=正常
 * </p>
 *
 * @author gugu
 */
@Data
public class UpdateUserStatusDTO {

    /** 目标用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 状态：0=冻结 / 1=正常 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值只能为 0 或 1")
    @Max(value = 1, message = "状态值只能为 0 或 1")
    private Integer status;
}