package com.example.skillsystem.service;

import com.example.skillsystem.dto.ProductDTO;
import com.example.skillsystem.vo.Result;

import java.util.List;

public interface ProductService {
    
    /**
     * 获取商品信息
     * 
     * @param id 商品ID
     * @return 商品DTO
     */
    ProductDTO getProductById(Long id);
    
    /**
     * 获取所有商品列表
     * 
     * @return 商品列表
     */
    List<ProductDTO> getAllProducts();
    
    /**
     * 根据状态获取商品列表
     * 
     * @param status 商品状态（1-上架，0-下架）
     * @return 商品列表
     */
    List<ProductDTO> getProductsByStatus(Integer status);
    
    /**
     * 创建新商品
     * 
     * @param productDTO 商品信息
     * @return 创建结果
     */
    Result<ProductDTO> createProduct(ProductDTO productDTO);
    
    /**
     * 更新商品信息
     * 
     * @param id 商品ID
     * @param productDTO 商品信息
     * @return 更新结果
     */
    Result<ProductDTO> updateProduct(Long id, ProductDTO productDTO);
    
    /**
     * 下架商品（逻辑删除）
     * 
     * @param id 商品ID
     * @return 操作结果
     */
    Result<Boolean> removeProduct(Long id);
    
    /**
     * 重新上架商品
     * 
     * @param id 商品ID
     * @return 操作结果
     */
    Result<Boolean> restoreProduct(Long id);
    
    /**
     * 扣减商品库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否成功
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 扣减商品库存（带订单号）
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean deductStock(Long productId, Integer quantity, String orderNo);

    /**
     * 增加商品库存
     *
     * @param productId 商品ID
     * @param quantity 增加数量
     * @return 是否成功
     */
    boolean increaseStock(Long productId, Integer quantity);

    /**
     * 增加商品库存（带订单号）
     *
     * @param productId 商品ID
     * @param quantity 增加数量
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean increaseStock(Long productId, Integer quantity, String orderNo);
    
    /**
     * 清除商品缓存
     * 
     * @param productId 商品ID
     */
    void clearProductCache(Long productId);
} 