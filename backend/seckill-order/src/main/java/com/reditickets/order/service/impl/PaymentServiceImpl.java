package com.reditickets.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.order.entity.Order;
import com.reditickets.order.entity.Payment;
import com.reditickets.order.mapper.PaymentMapper;
import com.reditickets.order.service.OrderService;
import com.reditickets.order.service.PaymentService;
import com.reditickets.order.vo.PaymentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 支付流水服务实现类
 * <p>
 * 处理支付流水创建、支付成功/失败回调和支付流水查询
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderService orderService;

    public PaymentServiceImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Result<PaymentVO> createPayment(Long orderId, Integer payChannel) {
        Order order = orderService.getById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            return Result.fail(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 0) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(), "订单状态不允许支付");
        }

        Payment existingPayment = getPaymentByOrderIdInternal(orderId);
        if (existingPayment != null && existingPayment.getPayStatus() == 1) {
            PaymentVO vo = new PaymentVO();
            BeanUtils.copyProperties(existingPayment, vo);
            vo.setOrderNo(order.getOrderNo());
            return Result.success(vo);
        }
        if (existingPayment != null && existingPayment.getPayStatus() == 0) {
            PaymentVO vo = new PaymentVO();
            BeanUtils.copyProperties(existingPayment, vo);
            vo.setOrderNo(order.getOrderNo());
            return Result.success(vo);
        }

        String paymentNo = "PY" + LocalDateTime.now().format(DTF)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(orderId);
        payment.setUserId(order.getUserId());
        payment.setPayAmount(order.getPayAmount());
        payment.setPayChannel(payChannel);
        payment.setPayStatus(0);

        boolean saved = this.save(payment);
        if (!saved) {
            return Result.fail(ResultCode.INTERNAL_ERROR);
        }

        log.info("支付流水创建成功: paymentNo={}, orderId={}, amount={}", paymentNo, orderId, order.getPayAmount());

        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        vo.setOrderNo(order.getOrderNo());
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentVO> handlePaySuccess(Long orderId, String tradeNo) {
        Order order = orderService.getById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            return Result.fail(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 0) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(),
                    "订单状态：" + order.getOrderStatus() + "，不允许重复支付");
        }

        Payment payment = getPaymentByOrderIdInternal(orderId);
        if (payment == null) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(), "支付流水不存在");
        }
        if (payment.getPayStatus() != 0) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(),
                    "支付流水状态：" + payment.getPayStatus() + "，不允许重复处理");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setPayStatus(1);
        payment.setTradeNo(tradeNo);
        payment.setPayTime(now);
        payment.setCallbackTime(now);
        this.updateById(payment);

        order.setOrderStatus(1);
        order.setPayTime(now);
        orderService.updateById(order);

        log.info("支付成功: orderId={}, tradeNo={}, amount={}", orderId, tradeNo, payment.getPayAmount());

        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        vo.setOrderNo(order.getOrderNo());
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentVO> handlePayFailed(Long orderId, String failReason) {
        Payment payment = getPaymentByOrderIdInternal(orderId);
        if (payment == null) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(), "支付流水不存在");
        }
        if (payment.getPayStatus() != 0) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(),
                    "支付流水状态：" + payment.getPayStatus() + "，不允许重复处理");
        }

        payment.setPayStatus(2);
        payment.setCallbackTime(LocalDateTime.now());
        this.updateById(payment);

        log.warn("支付失败: orderId={}, failReason={}", orderId, failReason);

        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        Order order = orderService.getById(orderId);
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }
        return Result.success(vo);
    }

    @Override
    public Result<PaymentVO> getPaymentByOrderId(Long orderId) {
        Payment payment = getPaymentByOrderIdInternal(orderId);
        if (payment == null) {
            return Result.fail(ResultCode.PAYMENT_FAILED.getCode(), "支付流水不存在");
        }

        Order order = orderService.getById(orderId);
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
        }
        return Result.success(vo);
    }

    private Payment getPaymentByOrderIdInternal(Long orderId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId)
                .eq(Payment::getIsDeleted, 0);
        List<Payment> payments = this.list(wrapper);
        return payments.isEmpty() ? null : payments.get(0);
    }
}