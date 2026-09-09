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
}