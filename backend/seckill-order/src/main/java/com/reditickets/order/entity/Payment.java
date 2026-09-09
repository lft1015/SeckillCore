package com.reditickets.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.reditickets.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_payment")
@ApiModel(value = "Payment", description = "支付流水")
public class Payment extends BaseEntity {

    @ApiModelProperty("支付编号（业务唯一标识）")
    @TableField("payment_no")
    private String paymentNo;

    @ApiModelProperty("关联订单ID")
    @TableField("order_id")
    private Long orderId;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty("支付金额")
    @TableField("pay_amount")
    private BigDecimal payAmount;

    @ApiModelProperty("支付渠道：0微信 / 1支付宝")
    @TableField("pay_channel")
    private Integer payChannel;

    @ApiModelProperty("支付状态：0待支付 / 1支付成功 / 2支付失败 / 3已退款")
    @TableField("pay_status")
    private Integer payStatus;

    @ApiModelProperty("第三方交易号")
    @TableField("trade_no")
    private String tradeNo;

    @ApiModelProperty("支付时间")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @ApiModelProperty("回调时间")
    @TableField("callback_time")
    private LocalDateTime callbackTime;
}