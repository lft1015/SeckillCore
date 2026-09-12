package com.reditickets.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reditickets.common.result.Result;
import com.reditickets.product.dto.DeductStockDTO;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.service.ProductService;
import com.reditickets.product.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品控制层（严格 RESTful + 版本命名）
 * <p>
 * 资源设计：product（商品）用于查询，internal/product 用于内部 Feign 调用
 * 对外接口统一使用 /api/v1/ 前缀，通过 HTTP 方法表达操作语义
 * </p>
 *
 * @author gugu
 */
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==================== 商品资源：/api/v1/product ====================

    /**
     * 商品列表分页查询
     * <p>
     * GET /api/v1/product/list —— 分页查询商品集合，支持关键字搜索和状态筛选
     * </p>
     *
     * @param dto 分页查询参数（页码、每页条数、关键字、状态）
     * @return 统一响应结果（包含分页商品列表）
     */
    @GetMapping("/api/v1/product/list")
    public Result<Page<ProductVO>> list(@Valid ProductListDTO dto) {
        return productService.listProducts(dto);
    }

    /**
     * 商品详情查询
     * <p>
     * GET /api/v1/product/detail/{id} —— 根据商品ID查询单品详情
     * </p>
     *
     * @param id 商品ID
     * @return 统一响应结果（包含商品详情视图对象）
     */
    @GetMapping("/api/v1/product/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // ==================== 内部 Feign 接口 ====================

    /**
     * 库存扣减（供秒杀模块 Feign 调用）
     * <p>
     * POST /api/v1/internal/product/deductStock —— 扣减商品可用库存
     * </p>
     *
     * @param dto 扣减请求（商品ID + 扣减数量）
     * @return 统一响应结果
     */
    @PostMapping("/api/v1/internal/product/deductStock")
    public Result<Void> deductStock(@Valid @RequestBody DeductStockDTO dto) {
        return productService.deductStock(dto.getProductId(), dto.getQuantity());
    }
}