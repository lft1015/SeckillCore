package com.reditickets.common.exception;

import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 使用 @RestControllerAdvice 统一拦截所有 Controller 层抛出的异常，
 * 将异常转换为统一的 Result 响应格式返回给前端
 * </p>
 *
 * @author gugu
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * <p>
     * 拦截 BusinessException，返回业务错误码和提示信息
     * </p>
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @Valid 参数校验异常（JSON 请求体）
     * <p>
     * 拦截 MethodArgumentNotValidException，收集所有字段校验失败信息并返回
     * </p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理参数绑定异常（表单请求体）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理 @RequestParam / @PathVariable 校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 处理数据库唯一键冲突异常
     * <p>
     * 高并发场景下，应用层先查后插的 TOCTOU 窗口期内可能出现并发插入同一条记录，
     * 数据库唯一索引作为最后防线抛出 DuplicateKeyException，此处拦截并解析为友好提示
     * </p>
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        String message = e.getMessage();
        if (message != null && message.contains("uk_username")) {
            return Result.fail(ResultCode.USERNAME_EXISTS);
        }
        if (message != null && message.contains("uk_phone")) {
            return Result.fail(ResultCode.PHONE_EXISTS);
        }
        log.warn("数据冲突: {}", message);
        return Result.fail(ResultCode.CONFLICT);
    }

    /**
     * 处理未预期的系统异常（兜底处理）
     * <p>
     * 捕获所有未被上述处理器处理的异常，返回 500 内部错误
     * </p>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}