package com.example.skillsystem.service;

import com.example.skillsystem.entity.StockLock;

import java.util.List;

/**
 * 库存锁定服务接口
 */
public interface StockLockService {
    
    /**
     * 锁定库存
     * @param productId 商品ID
     * @param orderNo 订单号
     * @param quantity 锁定数量
     * @return 是否锁定成功
     */
    boolean lockStock(Long productId, String orderNo, Integer quantity);
    
    /**
     * 释放库存锁定（取消订单时调用）
     * @param orderNo 订单号
     * @return 是否释放成功
     */
    boolean releaseStockLock(String orderNo);
    
    /**
     * 扣减锁定的库存（支付成功时调用）
     * @param orderNo 订单号
     * @return 是否扣减成功
     */
    boolean deductLockedStock(String orderNo);
    
    /**
     * 获取商品的总锁定库存
     * @param productId 商品ID
     * @return 锁定库存数量
     */
    Integer getTotalLockedQuantity(Long productId);
    
    /**
     * 清理过期的库存锁定
     * @return 清理的记录数
     */
    int cleanExpiredLocks();
    
    /**
     * 获取所有过期的库存锁定记录
     * @return 过期的库存锁定记录列表
     */
    List<StockLock> getExpiredLocks();
}
