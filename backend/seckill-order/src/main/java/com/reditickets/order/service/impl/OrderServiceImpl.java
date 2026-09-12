package com.reditickets.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.order.dto.CreateOrderDTO;
import com.reditickets.order.dto.OrderListDTO;
import com.reditickets.order.entity.Order;
import com.reditickets.order.mapper.OrderMapper;
import com.reditickets.order.service.OrderService;
import com.reditickets.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 订单服务实现类
 * <p>
 * 继承 MyBatis Plus ServiceImpl 基类，实现订单创建、查询等核心业务逻辑
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 创建秒杀订单实现
     * <p>
     * 步骤：生成订单号 → 构建 Order 实体 → this.save() 保存 → 转换为 OrderVO 返回
     * </p>
     */
    @Override
    public Result<OrderVO> createOrder(CreateOrderDTO dto) {
        Order order = createOrderInternal(dto);

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return Result.success(vo);
    }

    @Override
    public Order createOrderInternal(CreateOrderDTO dto) {
        String orderNo = "SK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(dto.getUserId());
        order.setActivityId(dto.getActivityId());
        order.setProductId(dto.getProductId());
        order.setSeckillPrice(dto.getSeckillPrice());
        order.setSeckillLogId(dto.getSeckillLogId());
        order.setQuantity(dto.getQuantity());
        order.setPayAmount(dto.getSeckillPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setOrderStatus(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));

        boolean saved = this.save(order);
        if (!saved) {
            throw new RuntimeException("订单保存失败");
        }

        log.info("订单创建成功 orderNo={} userId={} amount={}", orderNo, dto.getUserId(), order.getPayAmount());
        return order;
    }

    /**
     * 根据订单ID查询订单详情实现
     * <p>
     * 步骤：this.getById() 查询 → 校验存在 → BeanUtils 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<OrderVO> getOrderById(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            return Result.fail(ResultCode.ORDER_NOT_FOUND);
        }

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return Result.success(vo);
    }

    /**
     * 根据订单号查询订单详情实现
     * <p>
     * 步骤：LambdaQueryWrapper 按 order_no 精确查询 → 校验存在 → BeanUtils 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<OrderVO> getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo)
                .eq(Order::getIsDeleted, 0);

        Order order = this.getOne(wrapper);
        if (order == null) {
            return Result.fail(ResultCode.ORDER_NOT_FOUND);
        }

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return Result.success(vo);
    }

    /**
     * 分页查询用户订单列表实现
     * <p>
     * 步骤：构建分页对象 → LambdaQueryWrapper 按用户ID筛选 → 分页查询 → Page.convert() 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<Page<OrderVO>> listUserOrders(OrderListDTO dto) {
        Page<Order> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, dto.getUserId())
                .eq(Order::getIsDeleted, 0)
                .orderByDesc(Order::getCreateTime);

        this.page(page, wrapper);

        Page<OrderVO> voPage = (Page<OrderVO>) page.convert(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        });

        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            return Result.fail(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 0) {
            log.info("[取消订单] 订单状态非待支付，跳过: orderId={}, status={}", orderId, order.getOrderStatus());
            return Result.success();
        }

        order.setOrderStatus(2);
        boolean updated = this.updateById(order);
        if (!updated) {
            return Result.fail(ResultCode.ORDER_CANCEL_FAILED);
        }

        log.info("[取消订单] 超时取消: orderId={}, orderNo={}", orderId, order.getOrderNo());

        try {
            rocketMQTemplate.convertAndSend("order-stock-rollback-topic",
                    order.getOrderNo());
            log.info("[取消订单] 库存回滚消息已发送: orderNo={}", order.getOrderNo());
        } catch (Exception e) {
            log.error("[取消订单] 库存回滚消息发送失败: orderNo={}", order.getOrderNo(), e);
        }

        return Result.success();
    }
}