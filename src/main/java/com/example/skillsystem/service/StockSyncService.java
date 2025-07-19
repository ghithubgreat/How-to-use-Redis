package com.example.skillsystem.service;

import java.util.Map;

public interface StockSyncService {
    
    /**
     * 同步指定商品的库存
     * @param productId 商品ID
     */
    void syncStockByProductId(Long productId);
    
    /**
     * 同步所有商品的库存
     */
    void syncAllStocks();
    
    /**
     * 处理未同步的库存日志
     */
    void processUnsyncedLogs();
    
    /**
     * 获取库存同步状态
     * @return 同步状态信息
     */
    Map<String, Object> getStockSyncStatus();
} 