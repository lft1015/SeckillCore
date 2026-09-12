package com.reditickets.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reditickets.common.result.Result;
import com.reditickets.common.result.ResultCode;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.entity.Product;
import com.reditickets.product.mapper.ProductMapper;
import com.reditickets.product.service.ProductService;
import com.reditickets.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类
 * <p>
 * 继承 MyBatis Plus ServiceImpl 基类，实现商品分页查询、详情查询和库存扣减等核心业务逻辑
 * </p>
 *
 * @author gugu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_INFO_CACHE_KEY = "product:info:";
    private static final String STOCK_CACHE_KEY = "stock:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 分页查询商品列表实现
     * <p>
     * 步骤：构建分页对象 → LambdaQueryWrapper 条件查询 → 分页查询 → Page.convert() 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<Page<ProductVO>> listProducts(ProductListDTO dto) {
        Page<Product> page = new Page<>(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getIsDeleted, 0);
        if (dto.getStatus() != null) {
            wrapper.eq(Product::getStatus, dto.getStatus());
        }
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            wrapper.like(Product::getProductName, dto.getKeyword());
        }
        wrapper.orderByDesc(Product::getCreateTime);

        this.page(page, wrapper);

        Page<ProductVO> voPage = (Page<ProductVO>) page.convert(product -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(product, vo);
            return vo;
        });

        return Result.success(voPage);
    }

    /**
     * 商品详情查询实现
     * <p>
     * 步骤：查 Redis 缓存 → 未命中则查数据库 → 校验存在且未删除 → 回写缓存 → BeanUtils 转换 VO → 返回
     * </p>
     */
    @Override
    public Result<ProductVO> getProductById(Long id) {
        String cacheKey = PRODUCT_INFO_CACHE_KEY + id;

        ProductVO cached = getProductFromCache(cacheKey);
        if (cached != null) {
            return Result.success(cached);
        }

        Product product = this.getById(id);
        if (product == null || product.getIsDeleted() == 1) {
            return Result.fail(ResultCode.PRODUCT_NOT_FOUND);
        }

        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        setProductToCache(cacheKey, vo);

        return Result.success(vo);
    }

    /**
     * 商品库存扣减实现
     * <p>
     * 步骤：LambdaUpdateWrapper 乐观锁扣减 → 校验扣减结果 → 同步 Redis 缓存 → 返回
     * </p>
     */
    @Override
    public Result<Void> deductStock(Long productId, Integer quantity) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, productId)
                .ge(Product::getAvailableStock, quantity)
                .eq(Product::getIsDeleted, 0)
                .setSql("available_stock = available_stock - " + quantity);

        boolean success = this.update(wrapper);
        if (!success) {
            return Result.fail(ResultCode.STOCK_NOT_ENOUGH);
        }

        stringRedisTemplate.opsForValue().decrement(STOCK_CACHE_KEY + productId, quantity);

        return Result.success();
    }

    private ProductVO getProductFromCache(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, ProductVO.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("商品缓存反序列化失败 key={}", cacheKey, e);
        }
        return null;
    }

    private void setProductToCache(String cacheKey, ProductVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("商品缓存序列化失败 key={}", cacheKey, e);
        }
    }
}