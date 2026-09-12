package com.reditickets.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单列表查询请求数据传输对象（DTO）
 * <p>
 * 封装订单分页查询的请求参数，支持按用户ID筛选
 * </p>
 *
 * @author gugu
 */
@Data
public class OrderListDTO {

    /** 当前页码，默认第1页 */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /** 每页显示条数，默认10条 */
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数最多100条")
    private Integer size = 10;

    /** 用户ID，用于筛选指定用户的订单 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}