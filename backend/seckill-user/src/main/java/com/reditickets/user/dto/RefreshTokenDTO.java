package com.reditickets.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 刷新请求数据传输对象（DTO）
 * <p>
 * 封装客户端使用刷新令牌续期访问令牌的请求参数
 * </p>
 *
 * @author gugu
 */
@Data
public class RefreshTokenDTO {

    /** 刷新令牌 */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}