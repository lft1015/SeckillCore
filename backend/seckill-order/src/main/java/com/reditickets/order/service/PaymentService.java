package com.reditickets.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.order.entity.Payment;
import com.reditickets.order.vo.PaymentVO;

/**
 * 支付流水服务接口
 * <p>
 * 提供支付流水创建、支付回调和支付流水查询等业务逻辑定义
 * </p>
 *
 * @author gugu
 */
public interface PaymentService extends IService<Payment> {

    /**
     * 用户发起支付
     * <p>
     * 创建支付流水记录（状态=待支付），返回支付信息和唤起参数
     * </p>
     *
     * @param orderId 订单ID
     * @param payChannel 支付渠道：0微信 / 1支付宝
     * @return 支付流水视图对象
     */
    Result<PaymentVO> createPayment(Long orderId, Integer payChannel);

    /**
     * 支付成功回调处理
     * <p>
     * 更新支付流水状态为支付成功，同时更新订单状态为已支付
     * </p>
     *
     * @param orderId 订单ID
     * @param tradeNo 第三方交易号
     * @return 更新后的支付流水
     */
    Result<PaymentVO> handlePaySuccess(Long orderId, String tradeNo);

    /**
     * 支付失败回调处理
     * <p>
     * 更新支付流水状态为支付失败
     * </p>
     *
     * @param orderId 订单ID
     * @param failReason 失败原因
     * @return 更新后的支付流水
     */
    Result<PaymentVO> handlePayFailed(Long orderId, String failReason);

    /**
     * 按订单ID查询支付流水
     *
     * @param orderId 订单ID
     * @return 支付流水视图对象
     */
    Result<PaymentVO> getPaymentByOrderId(Long orderId);
}