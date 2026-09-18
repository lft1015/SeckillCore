package com.reditickets.interfaces.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.common.result.Result;
import com.reditickets.order.dto.OrderListDTO;
import com.reditickets.order.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 订单服务 Feign 客户端
 * <p>
 * 通过 Nacos 服务发现调用 seckill-order 微服务
 * </p>
 *
 * @author gugu
 */
@FeignClient(name = "seckill-order", url = "${seckill-order.url:http://localhost:8080}")
public interface OrderFeignClient {

    @GetMapping("/api/v1/order/list")
    Result<Page<OrderVO>> list(@SpringQueryMap OrderListDTO dto);

    @GetMapping("/api/v1/order/detail/{id}")
    Result<OrderVO> detail(@PathVariable("id") Long id);

    @GetMapping("/api/v1/order/no/{orderNo}")
    Result<OrderVO> detailByNo(@PathVariable("orderNo") String orderNo);
}