package com.example.skillsystem.service;

/**
 * 库存管理服务接口
 * 实现策略：
 * 1. 下单扣Redis：快速锁定库存，保障并发性能
 * 2. 支付扣数据库：最终一致性落地，Redis无需重复扣减
 * 3. 取消回滚Redis：释放锁定库存
 */
public interface StockManagementService {
    
    /**
     * 下单时锁定Redis库存
     * @param productId 商品ID
     * @param orderNo 订单号
     * @param quantity 锁定数量
     * @return 是否锁定成功
     */
    boolean lockRedisStock(Long productId, String orderNo, Integer quantity);
    
    /**
     * 支付成功时扣减数据库库存
     * @param orderNo 订单号
     * @return 是否扣减成功
     */
    boolean deductDatabaseStock(String orderNo);
    
    /**
     * 取消订单时回滚Redis库存
     * @param orderNo 订单号
     * @return 是否回滚成功
     */
    boolean rollbackRedisStock(String orderNo);
    
    /**
     * 同步数据库库存到Redis
     * @param productId 商品ID
     * @return 是否同步成功
     */
    boolean syncStockToRedis(Long productId);
    
    /**
     * 获取Redis中的可用库存
     * @param productId 商品ID
     * @return 可用库存数量
     */
    Integer getAvailableStock(Long productId);
    
    /**
     * 获取商品的锁定库存信息
     * @param productId 商品ID
     * @return 锁定库存数量
     */
    Integer getLockedStock(Long productId);
    
    /**
     * 清理过期的库存锁定
     * @return 清理的记录数
     */
    int cleanExpiredLocks();
}
