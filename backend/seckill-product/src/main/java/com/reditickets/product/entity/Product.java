package com.reditickets.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.reditickets.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 商品实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_product")
@ApiModel(value = "Product", description = "商品表")
public class Product extends BaseEntity {

    @ApiModelProperty("商品名称")
    @TableField("product_name")
    private String productName;

    @ApiModelProperty("商品描述")
    @TableField("description")
    private String description;

    @ApiModelProperty("原价")
    @TableField("price")
    private BigDecimal price;

    @ApiModelProperty("秒杀价")
    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    @ApiModelProperty("物理可用库存")
    @TableField("available_stock")
    private Integer availableStock;

    @ApiModelProperty("总库存")
    @TableField("total_stock")
    private Integer totalStock;

    @ApiModelProperty("商品主图URL")
    @TableField("image_url")
    private String imageUrl;

    @ApiModelProperty("状态：0下架 / 1上架")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("乐观锁版本号")
    @TableField("version")
    @Version
    private Integer version;
}