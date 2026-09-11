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

    /** 用户名，4-20位字母/数字/下划线，全局唯一 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在 4-20 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 密码，8-20位，必须包含大写字母、小写字母、数字、特殊字符 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~]).{8,20}$",
            message = "密码需包含大写字母、小写字母、数字和特殊字符")
    private String password;

    /** 确认密码，必须与 password 一致 */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 手机号，11位中国大陆手机号，全局唯一 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（选填），标准邮箱格式 */
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;
}