package com.reditickets.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.order.dto.CreateOrderDTO;
import com.reditickets.order.dto.OrderListDTO;
import com.reditickets.order.entity.Order;
import com.reditickets.order.vo.OrderVO;

/**
 * 订单服务接口
 * <p>
 * 继承 MyBatis Plus IService 接口，提供订单创建、查询等业务逻辑定义
 * </p>
 *
 * @author gugu
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建秒杀订单
     * <p>
     * 生成订单号、保存订单信息，并发送延时消息用于超时未支付自动取消
     * </p>
     *
     * @param dto 创建订单参数
     * @return 创建的订单信息
     */
    Result<OrderVO> createOrder(CreateOrderDTO dto);

    /**
     * 内部创建秒杀订单（供 MQ 消费者调用）
     * <p>
     * 与 {@link #createOrder(CreateOrderDTO)} 逻辑相同，
     * 但直接返回 Order 实体而非 Result 包装，异常直接抛出由调用方处理
     * </p>
     *
     * @param dto 创建订单参数
     * @return Order 实体
     */
    Order createOrderInternal(CreateOrderDTO dto);

    /**
     * 根据订单ID查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情视图对象
     */
    Result<OrderVO> getOrderById(Long orderId);

    /**
     * 根据订单号查询订单详情
     *
     * @param orderNo 订单号
     * @return 订单详情视图对象
     */
    Result<OrderVO> getOrderByOrderNo(String orderNo);

    /**
     * 分页查询用户订单列表
     *
     * @param dto 分页查询参数
     * @return 分页订单列表
     */
    Result<Page<OrderVO>> listUserOrders(OrderListDTO dto);

    /**
     * 超时取消订单
     * <p>
     * 仅处理状态为「待支付」的订单，将状态变更为「已取消」，
     * 并发送库存回滚消息到 RocketMQ
     * </p>
     *
     * @param orderId 订单ID
     * @return 取消结果
     */
    Result<Void> cancelOrder(Long orderId);
}