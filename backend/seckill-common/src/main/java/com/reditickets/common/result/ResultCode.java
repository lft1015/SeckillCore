package com.reditickets.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 * <p>
 * 定义系统中所有可能的响应状态码和对应的提示信息，
 * 按模块划分：通用（200-500）、用户（1000+）、商品（2000+）、活动（3000+）、秒杀（4000+）、订单（5000+）
 * </p>
 *
 * @author gugu
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未登录或 Token 已过期 */
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    /** 无权限访问 */
    FORBIDDEN(403, "无权限访问"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 数据冲突 */
    CONFLICT(409, "数据冲突"),
    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ========== 用户模块 1000+ ==========
    /** 用户不存在 */
    USER_NOT_FOUND(1001, "用户不存在"),
    /** 用户名已存在 */
    USERNAME_EXISTS(1002, "用户名已存在"),
    /** 密码错误 */
    PASSWORD_ERROR(1003, "密码错误"),
    /** 账号已被冻结 */
    USER_FROZEN(1004, "账号已被冻结"),
    /** 手机号已存在 */
    PHONE_EXISTS(1005, "手机号已存在"),
    /** 登录失败次数过多 */
    LOGIN_LOCKED(1006, "登录失败次数过多，请15分钟后再试"),
    /** 刷新令牌无效 */
    REFRESH_TOKEN_INVALID(1007, "无效的刷新令牌"),
    /** 刷新令牌已过期 */
    REFRESH_TOKEN_EXPIRED(1008, "登录已过期，请重新登录"),
    /** 刷新令牌已被使用 */
    REFRESH_TOKEN_REUSED(1009, "令牌已被使用，请重新登录"),
    /** 新密码与历史密码重复 */
    PASSWORD_HISTORY_REPEATED(1010, "新密码不能与最近使用过的密码相同"),
    /** 密码强度不足 */
    PASSWORD_TOO_WEAK(1011, "密码需包含大写字母、小写字母、数字和特殊字符"),
    /** 注册频率限制 */
    REGISTER_TOO_FREQUENT(1012, "注册过于频繁，请稍后再试"),
    /** 请求频率限制 */
    RATE_LIMITED(1013, "请求过于频繁，请稍后再试"),
    // ========== 商品模块 2000+ ==========
    /** 商品不存在 */
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    /** 库存不足 */
    STOCK_NOT_ENOUGH(2002, "库存不足"),

    // ========== 活动模块 3000+ ==========
    /** 活动不存在 */
    ACTIVITY_NOT_FOUND(3001, "活动不存在"),
    /** 活动未开始 */
    ACTIVITY_NOT_STARTED(3002, "活动未开始"),
    /** 活动已结束 */
    ACTIVITY_ENDED(3003, "活动已结束"),

    // ========== 秒杀模块 4000+ ==========
    /** 请勿重复秒杀 */
    SECKILL_REPEATED(4001, "请勿重复秒杀"),
    /** 秒杀失败 */
    SECKILL_FAILED(4002, "秒杀失败"),

    // ========== 订单模块 5000+ ==========
    /** 订单不存在 */
    ORDER_NOT_FOUND(5001, "订单不存在"),
    /** 订单取消失败 */
    ORDER_CANCEL_FAILED(5002, "订单取消失败");

    private final int code;
    private final String message;
}