package com.reditickets.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reditickets.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_order")
@ApiModel(value = "Order", description = "订单表")
public class Order extends BaseEntity {

    @ApiModelProperty("订单编号")
    @TableField("order_no")
    private String orderNo;

    @ApiModelProperty("用户ID（分库分表键）")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty("商品ID")
    @TableField("product_id")
    private Long productId;

    @ApiModelProperty("活动ID")
    @TableField("activity_id")
    private Long activityId;

    @ApiModelProperty("关联秒杀日志ID")
    @TableField("seckill_log_id")
    private Long seckillLogId;

    @ApiModelProperty("状态：0待支付/1已支付/2已取消/3已退款")
    @TableField("order_status")
    private Integer orderStatus;

    @ApiModelProperty("实付金额")
    @TableField("pay_amount")
    private BigDecimal payAmount;

    @ApiModelProperty("支付时间")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @ApiModelProperty("订单过期时间")
    @TableField("expire_time")
    private LocalDateTime expireTime;
}