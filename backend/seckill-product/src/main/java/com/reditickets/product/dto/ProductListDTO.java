package com.reditickets.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 商品列表查询请求数据传输对象（DTO）
 * <p>
 * 封装商品分页查询的请求参数，支持关键字搜索和状态筛选
 * </p>
 *
 * @author gugu
 */
@Data
public class ProductListDTO {

    /** 当前页码，默认第1页 */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /** 每页显示条数，默认10条 */
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数最多100条")
    private Integer size = 10;

    /** 搜索关键字（模糊匹配商品名） */
    private String keyword;

    /** 商品状态：0=下架，1=上架 */
    private Integer status;
}