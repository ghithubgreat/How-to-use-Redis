package com.example.skillsystem.service.impl;

import com.example.skillsystem.config.AppConfig;
import com.example.skillsystem.constants.RedisKeyPrefix;
import com.example.skillsystem.entity.Product;
import com.example.skillsystem.entity.StockLog;
import com.example.skillsystem.repository.ProductRepository;
import com.example.skillsystem.repository.StockLogRepository;
import com.example.skillsystem.service.MockRedisService;
import com.example.skillsystem.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncServiceImpl implements StockSyncService {
    
    private final ProductRepository productRepository;
    private final StockLogRepository stockLogRepository;
    private final MockRedisService mockRedisService;
    private final AppConfig appConfig;
    
    @Override
    @Transactional
    public void syncStockByProductId(Long productId) {
        log.info("开始同步商品库存, productId: {}", productId);
        
        // 获取数据库中的库存
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            log.error("商品不存在, productId: {}", productId);
            return;
        }
        
        Product product = productOpt.get();
        Integer dbStock = product.getStock();
        
        // 获取Redis中的库存
        String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
        Object redisStockObj = mockRedisService.get(stockKey);

        // 如果Redis中不存在库存，则写入
        if (redisStockObj == null) {
            mockRedisService.set(stockKey, dbStock, appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
            log.info("Redis中不存在库存，已写入, productId: {}, stock: {}", productId, dbStock);
            
            // 记录同步日志
            StockLog stockLog = StockLog.builder()
                    .productId(productId)
                    .beforeStock(dbStock)
                    .afterStock(dbStock)
                    .changeAmount(0)
                    .operationType("SYNC")
                    .createTime(LocalDateTime.now())
                    .synced(true)
                    .remark("Redis中不存在库存，初始化同步")
                    .build();
            stockLogRepository.save(stockLog);
            
                return;
            }
        
        // 获取Redis中的库存值
        Integer redisStock = Integer.parseInt(redisStockObj.toString());
        
        // 如果Redis和数据库库存不一致，以数据库为准进行同步
        if (!dbStock.equals(redisStock)) {
            log.warn("库存不一致, productId: {}, dbStock: {}, redisStock: {}", productId, dbStock, redisStock);
            
            // 更新Redis库存
            mockRedisService.set(stockKey, dbStock, appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
            
            // 记录同步日志
            StockLog stockLog = StockLog.builder()
                    .productId(productId)
                    .beforeStock(redisStock)
                    .afterStock(dbStock)
                    .changeAmount(dbStock - redisStock)
                    .operationType("SYNC")
                    .createTime(LocalDateTime.now())
                    .synced(true)
                    .remark("库存不一致，同步修复")
                    .build();
            stockLogRepository.save(stockLog);
            
            log.info("库存同步完成, productId: {}, 从 {} 修正为 {}", productId, redisStock, dbStock);
        } else {
            log.info("库存一致，无需同步, productId: {}, stock: {}", productId, dbStock);
        }
    }

    @Override
    @Transactional
    public void syncAllStocks() {
        log.info("开始全量同步库存");
        
        // 获取所有商品
        List<Product> products = productRepository.findAll();
        
        int totalCount = products.size();
        int syncCount = 0;
        int errorCount = 0;
        
            for (Product product : products) {
            try {
                syncStockByProductId(product.getId());
                syncCount++;
            } catch (Exception e) {
                log.error("同步库存失败, productId: {}, error: {}", product.getId(), e.getMessage(), e);
                errorCount++;
            }
        }
        
        log.info("全量同步库存完成, 总数: {}, 成功: {}, 失败: {}", totalCount, syncCount, errorCount);
    }

    @Override
    @Transactional
    public void processUnsyncedLogs() {
        log.info("开始处理未同步的库存日志");
        
        // 获取所有未同步的商品ID
        List<Long> productIds = stockLogRepository.findDistinctProductIdsWithUnsyncedLogs();
        
        for (Long productId : productIds) {
            try {
                // 获取该商品的所有未同步日志
                List<StockLog> logs = stockLogRepository.findByProductIdAndSyncedFalseOrderByCreateTimeAsc(productId);
                
                if (logs.isEmpty()) {
                    continue;
                }
                
                // 获取数据库中的当前库存
                Optional<Product> productOpt = productRepository.findById(productId);
                if (productOpt.isEmpty()) {
                    log.error("商品不存在, productId: {}", productId);
                    continue;
                }
                
                Product product = productOpt.get();
                Integer currentStock = product.getStock();
                
                // 获取Redis中的库存
                String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
                Object redisStockObj = mockRedisService.get(stockKey);

                // 如果Redis中不存在库存，则写入当前库存
                if (redisStockObj == null) {
                    mockRedisService.set(stockKey, currentStock, appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
                    log.info("Redis中不存在库存，已写入, productId: {}, stock: {}", productId, currentStock);
                } else {
                    // 应用所有未同步的日志
                    Integer redisStock = Integer.parseInt(redisStockObj.toString());
                    
                    for (StockLog log : logs) {
                        // 根据操作类型应用变更
                        if ("DEDUCT".equals(log.getOperationType())) {
                            redisStock -= log.getChangeAmount();
                        } else if ("INCREASE".equals(log.getOperationType())) {
                            redisStock += log.getChangeAmount();
                        }
                        
                        // 标记为已同步
                        log.setSynced(true);
                    }
                    
                    // 更新Redis库存
                    mockRedisService.set(stockKey, redisStock, appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
                    
                    // 保存更新后的日志
                    stockLogRepository.saveAll(logs);
                    
                    log.info("处理未同步日志完成, productId: {}, 处理日志数: {}, 最终Redis库存: {}", 
                            productId, logs.size(), redisStock);
                }
            } catch (Exception e) {
                log.error("处理未同步日志失败, productId: {}, error: {}", productId, e.getMessage(), e);
            }
        }
    }

    @Override
    public Map<String, Object> getStockSyncStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有未同步的日志数量
        long unsyncedCount = stockLogRepository.findBySyncedFalseOrderByCreateTimeAsc().size();
        result.put("unsyncedCount", unsyncedCount);
        
        // 获取今日同步日志数量
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow = today.plusDays(1);
        List<StockLog> todayLogs = stockLogRepository.findByCreateTimeBetween(today, tomorrow);
        result.put("todaySyncCount", todayLogs.size());
        
        // 获取不一致的商品列表
        List<Map<String, Object>> inconsistentProducts = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        
        for (Product product : products) {
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + product.getId();
            Object redisStockObj = mockRedisService.get(stockKey);
            
            // 如果Redis中不存在库存，或者与数据库库存不一致
            if (redisStockObj == null) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", product.getId());
                item.put("productName", product.getName());
                item.put("dbStock", product.getStock());
                item.put("redisStock", "不存在");
                inconsistentProducts.add(item);
            } else {
                Integer redisStock = Integer.parseInt(redisStockObj.toString());
                if (!product.getStock().equals(redisStock)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", product.getId());
                    item.put("productName", product.getName());
                    item.put("dbStock", product.getStock());
                    item.put("redisStock", redisStock);
                    inconsistentProducts.add(item);
                }
            }
        }
        
        result.put("inconsistentProducts", inconsistentProducts);
        result.put("inconsistentCount", inconsistentProducts.size());
        
        return result;
    }
} 