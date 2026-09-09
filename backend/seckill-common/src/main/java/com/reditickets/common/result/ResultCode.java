package com.reditickets.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_FROZEN(1004, "账号已被冻结"),

    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    STOCK_NOT_ENOUGH(2002, "库存不足"),

    ACTIVITY_NOT_FOUND(3001, "活动不存在"),
    ACTIVITY_NOT_STARTED(3002, "活动未开始"),
    ACTIVITY_ENDED(3003, "活动已结束"),

    SECKILL_REPEATED(4001, "请勿重复秒杀"),
    SECKILL_FAILED(4002, "秒杀失败"),

    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_CANCEL_FAILED(5002, "订单取消失败");

    private final int code;
    private final String message;
}