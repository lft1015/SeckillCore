package com.reditickets.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.common.result.Result;
import com.reditickets.order.dto.OrderListDTO;
import com.reditickets.order.service.OrderService;
import com.reditickets.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/list")
    public Result<Page<OrderVO>> list(OrderListDTO dto) {
        return orderService.listUserOrders(dto);
    }

    @GetMapping("/detail/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/no/{orderNo}")
    public Result<OrderVO> detailByNo(@PathVariable String orderNo) {
        return orderService.getOrderByOrderNo(orderNo);
    }
}