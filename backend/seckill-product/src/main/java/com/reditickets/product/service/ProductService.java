package com.reditickets.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.entity.Product;
import com.reditickets.product.vo.ProductVO;

public interface ProductService extends IService<Product> {

    Result<Page<ProductVO>> listProducts(ProductListDTO dto);

    Result<ProductVO> getProductById(Long id);

    Result<Void> deductStock(Long productId, Integer quantity);
}