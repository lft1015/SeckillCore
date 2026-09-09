package com.reditickets.user.vo;

import lombok.Data;

/**
 * 内部 Feign 调用返回的用户视图对象
 * <p>
 * 供其他微服务（订单/秒杀）通过 Feign 接口获取用户核心信息，
 * 不包含密码等敏感字段，仅包含必要信息
 * </p>
 *
 * @author gugu
 */
@Data
public class UserFeignVO {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 用户状态：0=冻结 / 1=正常 */
    private Integer status;
}