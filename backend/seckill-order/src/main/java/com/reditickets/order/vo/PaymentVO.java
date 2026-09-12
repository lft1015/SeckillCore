package com.reditickets.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付视图对象（VO）
 * <p>
 * 用于接口返回支付信息，包含支付金额、支付渠道、支付状态等字段
 * </p>
 *
 * @author gugu
 */
@Data
public class PaymentVO {

    /** 支付流水ID */
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 关联订单号 */
    private String orderNo;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 支付渠道：0=微信，1=支付宝 */
    private Integer payChannel;

    /** 支付状态：0=待支付，1=支付成功，2=支付失败，3=已退款 */
    private Integer payStatus;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}