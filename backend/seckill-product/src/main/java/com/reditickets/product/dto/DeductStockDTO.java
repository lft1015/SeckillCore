package com.reditickets.product.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 库存扣减请求数据传输对象（DTO）
 * <p>
 * 用于内部 Feign 接口调用，传递商品ID和扣减数量
 * </p>
 *
 * @author gugu
 */
@Data
public class DeductStockDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量必须大于0")
    private Integer quantity;
}