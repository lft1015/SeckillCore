package com.reditickets.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改用户信息请求数据传输对象（DTO）
 * <p>
 * 封装用户修改个人信息时提交的字段（头像、邮箱、手机号），
 * 所有字段均为选填，仅更新传入的非空字段
 * </p>
 *
 * @author gugu
 */
@Data
public class UpdateUserInfoDTO {

    /** 头像URL */
    private String avatar;

    /** 邮箱，标准邮箱格式 */
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    /** 手机号，11位中国大陆手机号，全局唯一 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}