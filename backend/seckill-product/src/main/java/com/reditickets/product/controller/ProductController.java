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

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public Result<Page<ProductVO>> list(ProductListDTO dto) {
        return productService.listProducts(dto);
    }

    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return productService.getProductById(id);
    }
}