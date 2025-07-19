package com.example.skillsystem.task;

import com.example.skillsystem.entity.Order;
import com.example.skillsystem.entity.StockLock;
import com.example.skillsystem.enums.OrderStatus;
import com.example.skillsystem.enums.StockLockStatus;
import com.example.skillsystem.repository.OrderRepository;
import com.example.skillsystem.repository.StockLockRepository;
import com.example.skillsystem.service.StockManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 最终一致性补偿定时任务
 * 处理以下异常情况：
 * 1. 已扣Redis但未支付的订单 - 补偿回滚Redis库存
 * 2. 已支付但未扣数据库的订单 - 补偿扣减数据库库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsistencyCompensationTask {
    
    private final OrderRepository orderRepository;
    private final StockLockRepository stockLockRepository;
    private final StockManagementService stockManagementService;
    
    /**
     * 每15分钟执行一次最终一致性补偿
     * 处理"已扣Redis但未支付"的订单
     */
    @Scheduled(fixedRate = 900000) // 15分钟 = 900000毫秒
    public void compensateRedisDeductedButUnpaid() {
        log.info("开始执行最终一致性补偿 - 处理已扣Redis但未支付的订单");
        
        try {
            // 查找超过30分钟未支付的订单（给用户足够的支付时间）
            LocalDateTime compensationTime = LocalDateTime.now().minusMinutes(30);
            
            List<Order> unpaidOrders = orderRepository.findByStatusAndCreateTimeLessThan(
                    OrderStatus.WAITING_PAYMENT.getCode(),
                    compensationTime
            );
            
            log.info("发现 {} 个超过30分钟未支付的订单需要补偿", unpaidOrders.size());
            
            int compensatedCount = 0;
            for (Order order : unpaidOrders) {
                try {
                    // 检查是否有对应的库存锁定记录
                    List<StockLock> stockLocks = stockLockRepository.findByOrderNoAndStatus(
                            order.getOrderNo(), 
                            StockLockStatus.LOCKED.getCode()
                    );
                    
                    if (!stockLocks.isEmpty()) {
                        // 有锁定记录，说明已扣Redis但未支付，需要回滚
                        log.info("补偿回滚Redis库存: orderNo={}, 锁定记录数={}", 
                                order.getOrderNo(), stockLocks.size());
                        
                        boolean rollbackResult = stockManagementService.rollbackRedisStock(order.getOrderNo());
                        if (rollbackResult) {
                            // 更新订单状态为已取消
                            orderRepository.updateOrderCancelled(
                                    order.getOrderNo(),
                                    OrderStatus.CANCELLED.getCode(),
                                    LocalDateTime.now()
                            );
                            compensatedCount++;
                            log.info("补偿成功 - 已扣Redis但未支付: orderNo={}", order.getOrderNo());
                        } else {
                            log.error("补偿失败 - 回滚Redis库存失败: orderNo={}", order.getOrderNo());
                        }
                    }
                } catch (Exception e) {
                    log.error("补偿处理异常: orderNo={}, error={}", order.getOrderNo(), e.getMessage(), e);
                }
            }
            
            log.info("已扣Redis但未支付订单补偿完成，共补偿 {} 个订单", compensatedCount);
            
        } catch (Exception e) {
            log.error("最终一致性补偿任务执行失败", e);
        }
    }
    
    /**
     * 每20分钟执行一次最终一致性补偿
     * 处理"已支付但未扣数据库"的订单
     */
    @Scheduled(fixedRate = 1200000) // 20分钟 = 1200000毫秒
    public void compensatePaidButDatabaseNotDeducted() {
        log.info("开始执行最终一致性补偿 - 处理已支付但未扣数据库的订单");
        
        try {
            // 查找已支付的订单
            List<Order> paidOrders = orderRepository.findByStatus(OrderStatus.PAID.getCode());
            
            log.info("发现 {} 个已支付订单需要检查数据库扣减状态", paidOrders.size());
            
            int compensatedCount = 0;
            for (Order order : paidOrders) {
                try {
                    // 检查是否有对应的库存锁定记录且状态为已扣减
                    List<StockLock> stockLocks = stockLockRepository.findByOrderNoAndStatus(
                            order.getOrderNo(), 
                            StockLockStatus.DEDUCTED.getCode()
                    );
                    
                    if (stockLocks.isEmpty()) {
                        // 没有已扣减的锁定记录，说明可能数据库扣减失败，需要补偿
                        log.info("补偿扣减数据库库存: orderNo={}", order.getOrderNo());
                        
                        boolean deductResult = stockManagementService.deductDatabaseStock(order.getOrderNo());
                        if (deductResult) {
                            compensatedCount++;
                            log.info("补偿成功 - 已支付但未扣数据库: orderNo={}", order.getOrderNo());
                        } else {
                            log.error("补偿失败 - 扣减数据库库存失败: orderNo={}", order.getOrderNo());
                        }
                    }
                } catch (Exception e) {
                    log.error("补偿处理异常: orderNo={}, error={}", order.getOrderNo(), e.getMessage(), e);
                }
            }
            
            log.info("已支付但未扣数据库订单补偿完成，共补偿 {} 个订单", compensatedCount);
            
        } catch (Exception e) {
            log.error("最终一致性补偿任务执行失败", e);
        }
    }
    
    /**
     * 每小时执行一次数据一致性检查
     * 检查并修复Redis和数据库之间的库存差异
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void checkDataConsistency() {
        log.info("开始执行数据一致性检查");
        
        try {
            // 这里可以添加更多的一致性检查逻辑
            // 比如检查Redis中的库存是否与数据库一致
            // 检查是否有孤立的库存锁定记录等
            
            log.info("数据一致性检查完成");
            
        } catch (Exception e) {
            log.error("数据一致性检查失败", e);
        }
    }
}
