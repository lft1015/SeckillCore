package com.reditickets.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求数据传输对象（DTO）
 * <p>
 * 封装用户注册时提交的信息，包含用户名、密码、手机号、邮箱，
 * 使用 Jakarta Validation 对参数进行格式校验
 * </p>
 *
 * @author gugu
 */
@Data
public class RegisterDTO {

    /** 用户名，长度 3-32 位 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
    private String username;

    /** 密码，长度 6-64 位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;

    /** 手机号，需符合中国大陆手机号格式 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（选填） */
    private String email;
}