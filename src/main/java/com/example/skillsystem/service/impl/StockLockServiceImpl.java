package com.example.skillsystem.service.impl;

import com.example.skillsystem.entity.Product;
import com.example.skillsystem.entity.StockLock;
import com.example.skillsystem.entity.StockLog;
import com.example.skillsystem.enums.StockLockStatus;
import com.example.skillsystem.enums.StockOperationType;
import com.example.skillsystem.repository.ProductRepository;
import com.example.skillsystem.repository.StockLockRepository;
import com.example.skillsystem.repository.StockLogRepository;
import com.example.skillsystem.service.StockLockService;
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
public class StockLockServiceImpl implements StockLockService {
    
    private final StockLockRepository stockLockRepository;
    private final ProductRepository productRepository;
    private final StockLogRepository stockLogRepository;
    
    @Override
    @Transactional
    public boolean lockStock(Long productId, String orderNo, Integer quantity) {
        log.info("开始锁定库存: productId={}, orderNo={}, quantity={}", productId, orderNo, quantity);
        
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
                return true; // 已经锁定过，返回成功
            }
            
            // 计算当前可用库存（总库存 - 已锁定库存）
            Integer totalLocked = stockLockRepository.getTotalLockedQuantity(productId);
            Integer availableStock = product.getStock() - (totalLocked != null ? totalLocked : 0);
            
            log.info("库存检查: 总库存={}, 已锁定={}, 可用库存={}, 需要锁定={}", 
                    product.getStock(), totalLocked, availableStock, quantity);
            
            // 检查可用库存是否足够
            if (availableStock < quantity) {
                log.error("库存不足: 可用库存={}, 需要锁定={}", availableStock, quantity);
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
                    .remark("订单创建时锁定库存")
                    .build();
            
            stockLockRepository.save(stockLock);
            
            // 记录库存操作日志
            StockLog stockLog = StockLog.builder()
                    .productId(productId)
                    .beforeStock(availableStock)
                    .afterStock(availableStock - quantity)
                    .changeAmount(-quantity)
                    .operationType(StockOperationType.LOCK.getCode())
                    .orderId(orderNo)
                    .createTime(LocalDateTime.now())
                    .synced(false)
                    .remark("锁定库存: " + quantity + " 件")
                    .build();
            
            stockLogRepository.save(stockLog);
            
            log.info("库存锁定成功: productId={}, orderNo={}, quantity={}", productId, orderNo, quantity);
            return true;
            
        } catch (Exception e) {
            log.error("库存锁定失败: productId={}, orderNo={}, quantity={}, error={}", 
                    productId, orderNo, quantity, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean releaseStockLock(String orderNo) {
        log.info("开始释放库存锁定: orderNo={}", orderNo);
        
        try {
            Optional<StockLock> lockOpt = stockLockRepository.findByOrderNo(orderNo);
            if (!lockOpt.isPresent()) {
                log.warn("未找到库存锁定记录: orderNo={}", orderNo);
                return true; // 没有锁定记录，认为释放成功
            }
            
            StockLock stockLock = lockOpt.get();
            
            // 检查状态
            if (!StockLockStatus.LOCKED.getCode().equals(stockLock.getStatus())) {
                log.warn("库存锁定状态不正确: orderNo={}, status={}", orderNo, stockLock.getStatus());
                return true; // 已经不是锁定状态，认为释放成功
            }
            
            // 更新锁定状态为已释放
            stockLock.setStatus(StockLockStatus.RELEASED.getCode());
            stockLock.setReleaseTime(LocalDateTime.now());
            stockLock.setRemark("订单取消，释放库存锁定");
            stockLockRepository.save(stockLock);
            
            // 记录库存操作日志
            StockLog stockLog = StockLog.builder()
                    .productId(stockLock.getProductId())
                    .beforeStock(0) // 这里不涉及实际库存变化
                    .afterStock(0)
                    .changeAmount(stockLock.getLockedQuantity())
                    .operationType(StockOperationType.ROLLBACK.getCode())
                    .orderId(orderNo)
                    .createTime(LocalDateTime.now())
                    .synced(false)
                    .remark("释放库存锁定: " + stockLock.getLockedQuantity() + " 件")
                    .build();
            
            stockLogRepository.save(stockLog);
            
            log.info("库存锁定释放成功: orderNo={}, quantity={}", orderNo, stockLock.getLockedQuantity());
            return true;
            
        } catch (Exception e) {
            log.error("释放库存锁定失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deductLockedStock(String orderNo) {
        log.info("开始扣减锁定库存: orderNo={}", orderNo);
        
        try {
            Optional<StockLock> lockOpt = stockLockRepository.findByOrderNo(orderNo);
            if (!lockOpt.isPresent()) {
                log.error("未找到库存锁定记录: orderNo={}", orderNo);
                return false;
            }
            
            StockLock stockLock = lockOpt.get();
            
            // 检查状态
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
            
            // 检查库存是否足够
            if (afterStock < 0) {
                log.error("库存不足，无法扣减: beforeStock={}, deductQuantity={}", beforeStock, stockLock.getLockedQuantity());
                return false;
            }
            
            // 扣减实际库存
            product.setStock(afterStock);
            product.setUpdateTime(LocalDateTime.now());
            productRepository.save(product);
            
            // 更新锁定状态为已扣减
            stockLock.setStatus(StockLockStatus.DEDUCTED.getCode());
            stockLock.setReleaseTime(LocalDateTime.now());
            stockLock.setRemark("支付成功，扣减库存");
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
                    .synced(false)
                    .remark("支付成功，扣减库存: " + stockLock.getLockedQuantity() + " 件")
                    .build();
            
            stockLogRepository.save(stockLog);
            
            log.info("锁定库存扣减成功: orderNo={}, quantity={}, beforeStock={}, afterStock={}", 
                    orderNo, stockLock.getLockedQuantity(), beforeStock, afterStock);
            return true;
            
        } catch (Exception e) {
            log.error("扣减锁定库存失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Integer getTotalLockedQuantity(Long productId) {
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
                // 释放过期的锁定
                lock.setStatus(StockLockStatus.RELEASED.getCode());
                lock.setReleaseTime(LocalDateTime.now());
                lock.setRemark("锁定过期，自动释放");
                stockLockRepository.save(lock);
                
                // 记录日志
                StockLog stockLog = StockLog.builder()
                        .productId(lock.getProductId())
                        .beforeStock(0)
                        .afterStock(0)
                        .changeAmount(lock.getLockedQuantity())
                        .operationType(StockOperationType.ROLLBACK.getCode())
                        .orderId(lock.getOrderNo())
                        .createTime(LocalDateTime.now())
                        .synced(false)
                        .remark("锁定过期，自动释放: " + lock.getLockedQuantity() + " 件")
                        .build();
                
                stockLogRepository.save(stockLog);
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
    
    @Override
    public List<StockLock> getExpiredLocks() {
        return stockLockRepository.findExpiredLocks(LocalDateTime.now());
    }
}
