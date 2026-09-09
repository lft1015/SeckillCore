package com.reditickets.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.entity.Product;
import com.reditickets.product.mapper.ProductMapper;
import com.reditickets.product.service.ProductService;
import com.reditickets.product.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public Result<Page<ProductVO>> listProducts(ProductListDTO dto) {
        // TODO: 1. 构建分页对象 Page<Product> page = new Page<>(dto.getPage(), dto.getSize());
        // TODO: 2. 使用 LambdaQueryWrapper 构建查询条件
        //    LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        //    wrapper.eq(Product::getIsDeleted, 0);
        //    if (dto.getStatus() != null) { wrapper.eq(Product::getStatus, dto.getStatus()); }
        //    if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
        //        wrapper.like(Product::getProductName, dto.getKeyword());
        //    }
        //    wrapper.orderByDesc(Product::getCreateTime);
        // TODO: 3. 执行分页查询 this.page(page, wrapper)
        // TODO: 4. 使用 Page.convert() 将 Product 转为 ProductVO
        //    Page<ProductVO> voPage = page.convert(product -> {
        //        ProductVO vo = new ProductVO();
        //        BeanUtils.copyProperties(product, vo);
        //        return vo;
        //    });
        // TODO: 5. 返回 Result.success(voPage)
        throw new UnsupportedOperationException("TODO: 实现商品列表查询");
    }

    @Override
    public Result<ProductVO> getProductById(Long id) {
        // TODO: 1. 使用 this.getById(id) 查询商品
        //    Product product = this.getById(id);
        // TODO: 2. 校验商品是否存在且未被删除
        //    if (product == null || product.getIsDeleted() == 1) {
        //        return Result.fail(ResultCode.PRODUCT_NOT_FOUND);
        //    }
        // TODO: 3. 使用 BeanUtils.copyProperties 转换为 ProductVO 返回
        //    ProductVO vo = new ProductVO();
        //    BeanUtils.copyProperties(product, vo);
        //    return Result.success(vo);
        throw new UnsupportedOperationException("TODO: 实现商品详情查询");
    }

    @Override
    public Result<Void> deductStock(Long productId, Integer quantity) {
        // TODO: 1. 使用 LambdaUpdateWrapper 乐观锁扣减库存
        //    LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        //    wrapper.eq(Product::getId, productId)
        //           .ge(Product::getAvailableStock, quantity)
        //           .eq(Product::getIsDeleted, 0);
        //    Product update = new Product();
        //    update.setAvailableStock(???); // 使用 SQL SET available_stock = available_stock - quantity
        //    boolean success = this.update(update, wrapper);
        // TODO: 2. 校验扣减结果
        //    if (!success) { return Result.fail(ResultCode.STOCK_NOT_ENOUGH); }
        // TODO: 3. 同步更新 Redis 库存缓存
        //    stringRedisTemplate.opsForValue().decrement("stock:" + productId, quantity);
        // TODO: 4. 返回成功
        //    return Result.success();
        throw new UnsupportedOperationException("TODO: 实现库存扣减");
    }
}