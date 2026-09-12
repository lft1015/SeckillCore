package com.reditickets.order.controller;

import com.reditickets.common.result.Result;
import com.reditickets.order.service.PaymentService;
import com.reditickets.order.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付控制层
 * <p>
 * 提供支付发起、支付回调和支付流水查询等 REST API 接口
 * </p>
 *
 * @author gugu
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 发起支付接口
     * <p>
     * 创建支付流水记录，返回支付信息供前端调起支付 SDK
     * </p>
     *
     * @param orderId 订单ID
     * @param payChannel 支付渠道：0=微信 / 1=支付宝
     * @return 支付流水视图对象
     */
    @PostMapping("/pay")
    public Result<PaymentVO> pay(@RequestParam Long orderId, @RequestParam Integer payChannel) {
        return paymentService.createPayment(orderId, payChannel);
    }

    /**
     * 支付成功回调接口
     * <p>
     * 第三方支付平台异步通知，更新支付流水和订单状态
     * </p>
     *
     * @param orderId 订单ID
     * @param tradeNo 第三方交易号
     * @return 更新后的支付流水
     */
    @PostMapping("/callback/success")
    public Result<PaymentVO> paySuccess(@RequestParam Long orderId, @RequestParam String tradeNo) {
        log.info("[支付回调] 支付成功: orderId={}, tradeNo={}", orderId, tradeNo);
        return paymentService.handlePaySuccess(orderId, tradeNo);
    }

    /**
     * 支付失败回调接口
     * <p>
     * 第三方支付平台异步通知支付失败，更新支付流水状态
     * </p>
     *
     * @param orderId 订单ID
     * @param failReason 失败原因
     * @return 更新后的支付流水
     */
    @PostMapping("/callback/fail")
    public Result<PaymentVO> payFailed(@RequestParam Long orderId, @RequestParam(required = false) String failReason) {
        log.warn("[支付回调] 支付失败: orderId={}, failReason={}", orderId, failReason);
        return paymentService.handlePayFailed(orderId, failReason);
    }

    /**
     * 支付流水查询接口（按订单ID）
     * <p>
     * 用户在订单详情页查看支付状态
     * </p>
     *
     * @param orderId 订单ID
     * @return 支付流水视图对象
     */
    @GetMapping("/info/{orderId}")
    public Result<PaymentVO> paymentInfo(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }
}