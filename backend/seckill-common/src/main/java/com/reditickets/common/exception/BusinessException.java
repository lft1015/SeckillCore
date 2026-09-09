package com.reditickets.common.exception;

import com.reditickets.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 继承自 {@link RuntimeException}，用于在业务逻辑中抛出异常，
 * 由 {@link GlobalExceptionHandler} 统一拦截处理并转换为 {@link com.reditickets.common.result.Result} 响应。
 * </p>
 * <p>
 * <b>设计优势：</b>
 * <ul>
 *   <li><b>无需 try-catch</b>：Service 层直接抛出异常，由全局异常处理器统一捕获，减少模板代码</li>
 *   <li><b>语义清晰</b>：通过 {@link ResultCode} 枚举携带业务错误码，前端可根据错误码做差异化处理</li>
 *   <li><b>灵活扩展</b>：支持自定义错误信息，适用于需要动态拼接消息的场景</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 场景1：使用预定义错误码
 * if (user == null) {
 *     throw new BusinessException(ResultCode.USER_NOT_FOUND);
 * }
 *
 * // 场景2：覆盖默认错误信息
 * if (stock < quantity) {
 *     throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "商品库存不足，剩余：" + stock);
 * }
 *
 * // 场景3：自定义错误码和消息
 * throw new BusinessException(5001, "下单频率过高，请稍后再试");
 * }</pre>
 * </p>
 *
 * @author gugu
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，对应 {@link ResultCode} 中的状态码 */
    private final int code;

    /**
     * 使用预定义的 {@link ResultCode} 枚举创建异常
     * <p>
     * 错误信息自动使用 ResultCode 中预设的 message
     * </p>
     *
     * @param resultCode 预定义的结果码枚举，包含错误码和默认消息
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用预定义错误码 + 自定义错误信息创建异常
     * <p>
     * 适用于需要动态拼接错误信息的场景，如库存不足时提示剩余数量
     * </p>
     *
     * @param resultCode 预定义的结果码枚举，提供错误码编号
     * @param message    自定义的异常描述信息，覆盖 ResultCode 中的默认消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 使用自定义错误码和错误信息创建异常
     * <p>
     * 适用于业务错误码不在 {@link ResultCode} 枚举中定义的临时场景
     * </p>
     *
     * @param code    自定义业务错误码
     * @param message 异常描述信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}