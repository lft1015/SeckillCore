package com.reditickets.user.vo;

import lombok.Data;

/**
 * 登录响应视图对象（VO）
 * <p>
 * 封装登录成功后返回的 JWT Token 和用户基本信息
 * </p>
 *
 * @author gugu
 */
@Data
public class LoginVO {

    /** JWT 认证令牌 */
    private String token;

    /** 用户基本信息（脱敏） */
    private UserVO userInfo;
}