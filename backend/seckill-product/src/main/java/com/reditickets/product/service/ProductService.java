package com.reditickets.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reditickets.common.result.Result;
import com.reditickets.product.dto.ProductListDTO;
import com.reditickets.product.entity.Product;
import com.reditickets.product.vo.ProductVO;

/**
 * 商品服务接口
 * <p>
 * 继承 MyBatis Plus IService 接口，提供商品查询和库存扣减等业务逻辑定义
 * </p>
 *
 * @author gugu
 */
public interface ProductService extends IService<Product> {

    /**
     * 分页查询商品列表
     *
     * @param dto 分页查询参数
     * @return 分页商品列表
     */
    Result<Page<ProductVO>> listProducts(ProductListDTO dto);

    /**
     * 根据商品ID查询商品详情
     *
     * @param id 商品ID
     * @return 商品详情视图对象
     */
    Result<ProductVO> getProductById(Long id);

    /**
     * 扣减商品库存
     * <p>
     * 使用乐观锁机制保证库存扣减的并发安全
     * </p>
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 扣减结果
     */
    Result<Void> deductStock(Long productId, Integer quantity);
}