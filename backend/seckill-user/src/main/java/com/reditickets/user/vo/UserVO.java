package com.reditickets.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象（VO）
 * <p>
 * 用于接口返回用户信息，已脱敏处理，不包含密码等敏感字段
 * </p>
 *
 * @author gugu
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像地址 */
    private String avatar;

    /** 用户状态：0=冻结，1=正常 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}