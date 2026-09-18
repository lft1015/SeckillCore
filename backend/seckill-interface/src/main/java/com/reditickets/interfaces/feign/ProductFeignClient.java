package com.reditickets.interfaces.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.common.result.Result;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.vo.ProductVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端
 * <p>
 * 通过 Nacos 服务发现调用 seckill-product 微服务
 * </p>
 *
 * @author gugu
 */
@FeignClient(name = "seckill-product", url = "http://localhost:8080", path = "/api/v1/product")
public interface ProductFeignClient {

    @GetMapping("/list")
    Result<Page<ProductVO>> list(@SpringQueryMap ProductListDTO dto);

    @GetMapping("/detail/{id}")
    Result<ProductVO> detail(@PathVariable("id") Long id);
}