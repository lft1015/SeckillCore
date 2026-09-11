package com.reditickets.user.vo;

import lombok.Data;

/**
 * 登录响应视图对象（VO）
 * <p>
 * 封装登录成功后返回的 JWT 访问令牌、刷新令牌和用户基本信息
 * </p>
 *
 * @author gugu
 */
@Data
public class LoginVO {

    /** JWT 访问令牌（有效期 2 小时） */
    private String token;

    /** JWT 刷新令牌（有效期 7 天，仅用于续期访问令牌） */
    private String refreshToken;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 头像URL */
    private String avatar;
}