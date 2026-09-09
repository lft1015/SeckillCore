package com.reditickets.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求数据传输对象（DTO）
 * <p>
 * 封装用户登录时提交的用户名和密码，使用 Jakarta Validation 进行参数校验
 * </p>
 *
 * @author gugu
 */
@Data
public class LoginDTO {

    /** 用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;
}