package com.reditickets.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.order.dto.CreateOrderDTO;
import com.reditickets.order.dto.OrderListDTO;
import com.reditickets.order.entity.Order;
import com.reditickets.order.vo.OrderVO;

public interface OrderService extends IService<Order> {

    Result<OrderVO> createOrder(CreateOrderDTO dto);

    Result<OrderVO> getOrderById(Long orderId);

    Result<OrderVO> getOrderByOrderNo(String orderNo);

    Result<Page<OrderVO>> listUserOrders(OrderListDTO dto);
}