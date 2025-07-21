package com.example.skillsystem.service;

import com.example.skillsystem.mq.StockMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Mock MQ服务
 * 模拟RabbitMQ的消息队列功能，用于演示MQ逻辑
 */
@Slf4j
@Service
public class MockMQService {

    private final ConcurrentLinkedQueue<StockMessage> deductionQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<StockMessage> rollbackQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    /**
     * 发送库存扣减消息
     */
    public void sendStockDeductionMessage(StockMessage message) {
        log.info("📤 [Mock MQ] 发送库存扣减消息: {}", message);
        deductionQueue.offer(message);
        
        // 异步处理消息（模拟MQ的异步特性）
        CompletableFuture.runAsync(() -> {
            try {
                // 模拟网络延迟
                Thread.sleep(100);
                processDeductionMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("处理扣减消息被中断", e);
            }
        }, executor);
    }

    /**
     * 发送库存回滚消息
     */
    public void sendStockRollbackMessage(StockMessage message) {
        log.info("📤 [Mock MQ] 发送库存回滚消息: {}", message);
        rollbackQueue.offer(message);
        
        // 异步处理消息
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                processRollbackMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("处理回滚消息被中断", e);
            }
        }, executor);
    }

    /**
     * 处理库存扣减消息
     */
    private void processDeductionMessage(StockMessage message) {
        log.info("📥 [Mock MQ] 开始处理库存扣减消息: {}", message);
        
        try {
            // 这里应该调用实际的消费者逻辑
            // 由于是Mock，我们只是记录日志
            log.info("✅ [Mock MQ] 库存扣减消息处理完成: productId={}, quantity={}, orderNo={}", 
                    message.getProductId(), message.getQuantity(), message.getOrderNo());
                    
        } catch (Exception e) {
            log.error("❌ [Mock MQ] 库存扣减消息处理失败: {}", message, e);
        }
    }

    /**
     * 处理库存回滚消息
     */
    private void processRollbackMessage(StockMessage message) {
        log.info("📥 [Mock MQ] 开始处理库存回滚消息: {}", message);
        
        try {
            log.info("✅ [Mock MQ] 库存回滚消息处理完成: productId={}, quantity={}, orderNo={}", 
                    message.getProductId(), message.getQuantity(), message.getOrderNo());
                    
        } catch (Exception e) {
            log.error("❌ [Mock MQ] 库存回滚消息处理失败: {}", message, e);
        }
    }

    /**
     * 获取队列状态信息
     */
    public String getQueueStatus() {
        return String.format("扣减队列: %d 条消息, 回滚队列: %d 条消息", 
                deductionQueue.size(), rollbackQueue.size());
    }

    /**
     * 清空所有队列
     */
    public void clearAllQueues() {
        deductionQueue.clear();
        rollbackQueue.clear();
        log.info("🧹 [Mock MQ] 已清空所有队列");
    }
}
