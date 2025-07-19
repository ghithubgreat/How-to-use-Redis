package com.example.skillsystem.service.impl;

import com.example.skillsystem.constants.RedisKeyPrefix;
import com.example.skillsystem.entity.Product;
import com.example.skillsystem.entity.StockLock;
import com.example.skillsystem.entity.StockLog;
import com.example.skillsystem.enums.StockLockStatus;
import com.example.skillsystem.enums.StockOperationType;
import com.example.skillsystem.repository.ProductRepository;
import com.example.skillsystem.repository.StockLockRepository;
import com.example.skillsystem.repository.StockLogRepository;
import com.example.skillsystem.service.MockRedisService;
import com.example.skillsystem.service.StockManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockManagementServiceImpl implements StockManagementService {
    
    private final MockRedisService mockRedisService;
    private final ProductRepository productRepository;
    private final StockLockRepository stockLockRepository;
    private final StockLogRepository stockLogRepository;
    
    @Override
    @Transactional
    public boolean lockRedisStock(Long productId, String orderNo, Integer quantity) {
        log.info("开始锁定Redis库存: productId={}, orderNo={}, quantity={}", productId, orderNo, quantity);
        
        try {
            // 检查商品是否存在
            Optional<Product> productOpt = productRepository.findById(productId);
            if (!productOpt.isPresent()) {
                log.error("商品不存在: productId={}", productId);
                return false;
            }
            
            Product product = productOpt.get();
            
            // 检查是否已经锁定过
            Optional<StockLock> existingLock = stockLockRepository.findByOrderNo(orderNo);
            if (existingLock.isPresent()) {
                log.warn("订单已锁定库存: orderNo={}", orderNo);
                return true;
            }
            
            // 确保Redis中有库存数据
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
            Object redisStock = mockRedisService.get(stockKey);
            Integer currentStock;

            if (redisStock == null) {
                // 同步数据库库存到Redis
                currentStock = product.getStock();
                mockRedisService.set(stockKey, currentStock);
                log.info("同步数据库库存到Redis: productId={}, stock={}", productId, currentStock);
            } else {
                if (redisStock instanceof Integer) {
                    currentStock = (Integer) redisStock;
                } else if (redisStock instanceof Long) {
                    currentStock = ((Long) redisStock).intValue();
                } else {
                    currentStock = Integer.parseInt(redisStock.toString());
                }
            }

            // 检查库存是否足够
            if (currentStock < quantity) {
                log.error("Redis库存不足: productId={}, 需要={}, 当前库存={}", productId, quantity, currentStock);
                return false;
            }

            // 原子性扣减Redis库存
            Long remainingStock = mockRedisService.decrBy(stockKey, quantity);
            if (remainingStock == null || remainingStock < 0) {
                // 库存不足，回滚Redis库存
                if (remainingStock != null && remainingStock < 0) {
                    mockRedisService.incrBy(stockKey, quantity);
                }
                log.error("Redis库存扣减失败: productId={}, 需要={}, 剩余={}", productId, quantity, remainingStock);
                return false;
            }
            
            // 创建库存锁定记录
            StockLock stockLock = StockLock.builder()
                    .productId(productId)
                    .orderNo(orderNo)
                    .lockedQuantity(quantity)
                    .status(StockLockStatus.LOCKED.getCode())
                    .createTime(LocalDateTime.now())
                    .expireTime(LocalDateTime.now().plusMinutes(30)) // 30分钟后过期
                    .remark("下单锁定Redis库存")
                    .build();
            
            stockLockRepository.save(stockLock);
            
            // 记录库存操作日志
            StockLog stockLog = StockLog.builder()
                    .productId(productId)
                    .beforeStock(remainingStock.intValue() + quantity)
                    .afterStock(remainingStock.intValue())
                    .changeAmount(-quantity)
                    .operationType(StockOperationType.LOCK.getCode())
                    .orderId(orderNo)
                    .createTime(LocalDateTime.now())
                    .synced(false)
                    .remark("下单锁定Redis库存: " + quantity + " 件")
                    .build();
            
            stockLogRepository.save(stockLog);
            
            log.info("Redis库存锁定成功: productId={}, orderNo={}, quantity={}, 剩余库存={}", 
                    productId, orderNo, quantity, remainingStock);
            return true;
            
        } catch (Exception e) {
            log.error("Redis库存锁定失败: productId={}, orderNo={}, quantity={}, error={}", 
                    productId, orderNo, quantity, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deductDatabaseStock(String orderNo) {
        log.info("开始扣减数据库库存: orderNo={}", orderNo);
        
        try {
            Optional<StockLock> lockOpt = stockLockRepository.findByOrderNo(orderNo);
            if (!lockOpt.isPresent()) {
                log.error("未找到库存锁定记录: orderNo={}", orderNo);
                return false;
            }
            
            StockLock stockLock = lockOpt.get();
            
            // 检查锁定状态
            if (!StockLockStatus.LOCKED.getCode().equals(stockLock.getStatus())) {
                log.error("库存锁定状态不正确: orderNo={}, status={}", orderNo, stockLock.getStatus());
                return false;
            }
            
            // 获取商品信息
            Optional<Product> productOpt = productRepository.findById(stockLock.getProductId());
            if (!productOpt.isPresent()) {
                log.error("商品不存在: productId={}", stockLock.getProductId());
                return false;
            }
            
            Product product = productOpt.get();
            Integer beforeStock = product.getStock();
            Integer afterStock = beforeStock - stockLock.getLockedQuantity();
            
            // 检查数据库库存是否足够
            if (afterStock < 0) {
                log.error("数据库库存不足，无法扣减: beforeStock={}, deductQuantity={}", 
                        beforeStock, stockLock.getLockedQuantity());
                return false;
            }
            
            // 扣减数据库库存
            product.setStock(afterStock);
            product.setUpdateTime(LocalDateTime.now());
            productRepository.save(product);
            
            // 更新锁定状态为已扣减
            stockLock.setStatus(StockLockStatus.DEDUCTED.getCode());
            stockLock.setReleaseTime(LocalDateTime.now());
            stockLock.setRemark("支付成功，扣减数据库库存");
            stockLockRepository.save(stockLock);
            
            // 记录库存操作日志
            StockLog stockLog = StockLog.builder()
                    .productId(stockLock.getProductId())
                    .beforeStock(beforeStock)
                    .afterStock(afterStock)
                    .changeAmount(-stockLock.getLockedQuantity())
                    .operationType(StockOperationType.DEDUCT.getCode())
                    .orderId(orderNo)
                    .createTime(LocalDateTime.now())
                    .synced(true)
                    .remark("支付成功，扣减数据库库存: " + stockLock.getLockedQuantity() + " 件")
                    .build();
            
            stockLogRepository.save(stockLog);
            
            log.info("数据库库存扣减成功: orderNo={}, quantity={}, beforeStock={}, afterStock={}", 
                    orderNo, stockLock.getLockedQuantity(), beforeStock, afterStock);
            return true;
            
        } catch (Exception e) {
            log.error("扣减数据库库存失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean rollbackRedisStock(String orderNo) {
        log.info("开始回滚Redis库存: orderNo={}", orderNo);
        
        try {
            Optional<StockLock> lockOpt = stockLockRepository.findByOrderNo(orderNo);
            if (!lockOpt.isPresent()) {
                log.warn("未找到库存锁定记录: orderNo={}", orderNo);
                return true; // 没有锁定记录，认为回滚成功
            }
            
            StockLock stockLock = lockOpt.get();
            
            // 检查状态
            if (!StockLockStatus.LOCKED.getCode().equals(stockLock.getStatus())) {
                log.warn("库存锁定状态不正确: orderNo={}, status={}", orderNo, stockLock.getStatus());
                return true; // 已经不是锁定状态，认为回滚成功
            }
            
            // 回滚Redis库存
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + stockLock.getProductId();
            Long newStock = mockRedisService.incrBy(stockKey, stockLock.getLockedQuantity().longValue());
            
            // 更新锁定状态为已释放
            stockLock.setStatus(StockLockStatus.RELEASED.getCode());
            stockLock.setReleaseTime(LocalDateTime.now());
            stockLock.setRemark("订单取消，回滚Redis库存");
            stockLockRepository.save(stockLock);
            
            // 记录库存操作日志
            StockLog stockLog = StockLog.builder()
                    .productId(stockLock.getProductId())
                    .beforeStock(newStock != null ? newStock.intValue() - stockLock.getLockedQuantity() : 0)
                    .afterStock(newStock != null ? newStock.intValue() : 0)
                    .changeAmount(stockLock.getLockedQuantity())
                    .operationType(StockOperationType.ROLLBACK.getCode())
                    .orderId(orderNo)
                    .createTime(LocalDateTime.now())
                    .synced(false)
                    .remark("订单取消，回滚Redis库存: " + stockLock.getLockedQuantity() + " 件")
                    .build();
            
            stockLogRepository.save(stockLog);
            
            log.info("Redis库存回滚成功: orderNo={}, quantity={}, 回滚后库存={}", 
                    orderNo, stockLock.getLockedQuantity(), newStock);
            return true;
            
        } catch (Exception e) {
            log.error("回滚Redis库存失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean syncStockToRedis(Long productId) {
        try {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (!productOpt.isPresent()) {
                log.error("商品不存在: productId={}", productId);
                return false;
            }
            
            Product product = productOpt.get();
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
            mockRedisService.set(stockKey, product.getStock());
            
            log.info("同步库存到Redis成功: productId={}, stock={}", productId, product.getStock());
            return true;
            
        } catch (Exception e) {
            log.error("同步库存到Redis失败: productId={}, error={}", productId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public Integer getAvailableStock(Long productId) {
        try {
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
            Object stock = mockRedisService.get(stockKey);
            if (stock instanceof Integer) {
                return (Integer) stock;
            } else if (stock instanceof Long) {
                return ((Long) stock).intValue();
            }
            return null;
        } catch (Exception e) {
            log.error("获取Redis库存失败: productId={}, error={}", productId, e.getMessage());
            return null;
        }
    }
    
    @Override
    public Integer getLockedStock(Long productId) {
        return stockLockRepository.getTotalLockedQuantity(productId);
    }
    
    @Override
    @Transactional
    public int cleanExpiredLocks() {
        log.info("开始清理过期的库存锁定");
        
        List<StockLock> expiredLocks = stockLockRepository.findExpiredLocks(LocalDateTime.now());
        int cleanedCount = 0;
        
        for (StockLock lock : expiredLocks) {
            try {
                // 回滚Redis库存
                rollbackRedisStock(lock.getOrderNo());
                cleanedCount++;
                
                log.info("清理过期锁定: orderNo={}, productId={}, quantity={}", 
                        lock.getOrderNo(), lock.getProductId(), lock.getLockedQuantity());
                
            } catch (Exception e) {
                log.error("清理过期锁定失败: orderNo={}, error={}", lock.getOrderNo(), e.getMessage());
            }
        }
        
        log.info("过期库存锁定清理完成，共清理 {} 条记录", cleanedCount);
        return cleanedCount;
    }
}
