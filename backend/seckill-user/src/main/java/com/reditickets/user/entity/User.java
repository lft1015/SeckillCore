package com.reditickets.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reditickets.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_user")
@ApiModel(value = "User", description = "用户表")
public class User extends BaseEntity {

    @ApiModelProperty("用户名")
    @TableField("username")
    private String username;

    @ApiModelProperty("密码（BCrypt加密）")
    @TableField("password")
    private String password;

    @ApiModelProperty("手机号")
    @TableField("phone")
    private String phone;

    @ApiModelProperty("邮箱")
    @TableField("email")
    private String email;

    @ApiModelProperty("头像URL")
    @TableField("avatar")
    private String avatar;

    @ApiModelProperty("状态：0冻结 / 1正常")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("最后登录IP")
    @TableField("last_login_ip")
    private String lastLoginIp;

    @ApiModelProperty("最后登录时间")
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
}