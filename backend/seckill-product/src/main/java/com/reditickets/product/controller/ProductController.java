package com.reditickets.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.common.result.Result;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.service.ProductService;
import com.reditickets.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品控制层
 * <p>
 * 提供商品列表分页查询和商品详情查询等 REST API 接口
 * </p>
 *
 * @author gugu
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品列表分页查询接口
     * <p>
     * 支持按关键字模糊搜索和按状态筛选，返回分页后的商品列表
     * </p>
     *
     * @param dto 分页查询参数（页码、每页条数、关键字、状态）
     * @return 分页商品列表
     */
    @GetMapping("/list")
    public Result<Page<ProductVO>> list(ProductListDTO dto) {
        return productService.listProducts(dto);
    }

    /**
     * 商品详情查询接口
     * <p>
     * 根据商品ID查询商品详细信息，包含秒杀价格、库存等
     * </p>
     *
     * @param id 商品ID
     * @return 商品详情视图对象
     */
    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return productService.getProductById(id);
    }
}