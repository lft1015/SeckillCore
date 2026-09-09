package com.reditickets.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象（VO）
 * <p>
 * 用于接口返回商品信息，包含原价、秒杀价、库存等完整字段
 * </p>
 *
 * @author gugu
 */
@Data
public class ProductVO {

    /** 商品ID */
    private Long id;

    /** 商品名称 */
    private String productName;

    /** 商品描述 */
    private String description;

    /** 原价 */
    private BigDecimal price;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 可用库存 */
    private Integer availableStock;

    /** 总库存 */
    private Integer totalStock;

    /** 商品图片URL */
    private String imageUrl;

    /** 商品状态：0=下架，1=上架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}