package com.reditickets.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求数据传输对象（DTO）
 * <p>
 * 封装用户修改密码时提交的旧密码、新密码和确认密码
 * </p>
 *
 * @author gugu
 */
@Data
public class UpdatePasswordDTO {

    /** 旧密码 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码，8-20位，至少含字母和数字 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    private String newPassword;

    /** 确认新密码，必须与 newPassword 一致 */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}