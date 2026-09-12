package com.reditickets.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.common.result.Result;
import com.reditickets.order.dto.CreateOrderDTO;
import com.reditickets.order.dto.OrderListDTO;
import com.reditickets.order.service.OrderService;
import com.reditickets.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制层
 * <p>
 * 提供订单列表查询、订单详情查询和内部订单创建等 REST API 接口
 * </p>
 *
 * @author gugu
 */
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 用户订单列表分页查询接口
     * <p>
     * 根据用户ID查询其所有订单，按创建时间倒序排列
     * </p>
     *
     * @param dto 分页查询参数（页码、每页条数、用户ID）
     * @return 分页订单列表
     */
    @GetMapping("/api/v1/order/list")
    public Result<Page<OrderVO>> list(@Valid OrderListDTO dto) {
        return orderService.listUserOrders(dto);
    }

    /**
     * 订单详情查询接口（按订单ID）
     * <p>
     * 根据订单ID查询订单详细信息
     * </p>
     *
     * @param id 订单ID
     * @return 订单详情视图对象
     */
    @GetMapping("/api/v1/order/detail/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    /**
     * 订单详情查询接口（按订单号）
     * <p>
     * 根据订单号查询订单详细信息，供秒杀结果查询使用
     * </p>
     *
     * @param orderNo 订单号
     * @return 订单详情视图对象
     */
    @GetMapping("/api/v1/order/no/{orderNo}")
    public Result<OrderVO> detailByNo(@PathVariable String orderNo) {
        return orderService.getOrderByOrderNo(orderNo);
    }

    /**
     * 创建秒杀订单接口（内部 Feign 调用）
     * <p>
     * 秒杀模块异步调用，创建订单并返回订单信息
     * </p>
     *
     * @param dto 创建订单参数
     * @return 创建的订单信息
     */
    @PostMapping("/api/v1/internal/order/create")
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return orderService.createOrder(dto);
    }
}