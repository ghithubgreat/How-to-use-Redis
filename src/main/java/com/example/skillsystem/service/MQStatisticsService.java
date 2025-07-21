package com.example.skillsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MQ消息统计服务
 * 用于统计和记录MQ消息发送情况
 */
@Slf4j
@Service
public class MQStatisticsService {

    // 消息计数器
    private final AtomicLong deductionMessageCount = new AtomicLong(0);
    private final AtomicLong rollbackMessageCount = new AtomicLong(0);
    
    // 消息日志列表（最多保留100条）
    private final List<MQLogEntry> messageLogs = new ArrayList<>();
    private static final int MAX_LOG_SIZE = 100;
    
    /**
     * 记录扣减消息发送
     */
    public void recordDeductionMessage(Long productId, Integer quantity, String orderNo, boolean success) {
        deductionMessageCount.incrementAndGet();
        
        String message = String.format("🔽 库存扣减消息: productId=%d, quantity=%d, orderNo=%s", 
                productId, quantity, orderNo);
        
        addLog(success ? "success" : "error", message);
        
        log.info("记录扣减消息统计: 总计={}, 当前消息={}", deductionMessageCount.get(), message);
    }
    
    /**
     * 记录回滚消息发送
     */
    public void recordRollbackMessage(Long productId, Integer quantity, String orderNo, boolean success) {
        rollbackMessageCount.incrementAndGet();
        
        String message = String.format("🔄 库存回滚消息: productId=%d, quantity=%d, orderNo=%s", 
                productId, quantity, orderNo);
        
        addLog(success ? "warning" : "error", message);
        
        log.info("记录回滚消息统计: 总计={}, 当前消息={}", rollbackMessageCount.get(), message);
    }
    
    /**
     * 获取扣减消息数量
     */
    public long getDeductionMessageCount() {
        return deductionMessageCount.get();
    }
    
    /**
     * 获取回滚消息数量
     */
    public long getRollbackMessageCount() {
        return rollbackMessageCount.get();
    }
    
    /**
     * 获取消息日志
     */
    public List<MQLogEntry> getMessageLogs() {
        synchronized (messageLogs) {
            return new ArrayList<>(messageLogs);
        }
    }
    
    /**
     * 清空统计和日志
     */
    public void clearStatistics() {
        deductionMessageCount.set(0);
        rollbackMessageCount.set(0);
        synchronized (messageLogs) {
            messageLogs.clear();
        }
        addLog("info", "🧹 MQ统计和日志已清空");
        log.info("MQ统计和日志已清空");
    }
    
    /**
     * 添加日志条目
     */
    private void addLog(String type, String message) {
        synchronized (messageLogs) {
            MQLogEntry logEntry = new MQLogEntry(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    type,
                    message
            );
            
            messageLogs.add(logEntry);
            
            // 保持日志数量在限制范围内
            if (messageLogs.size() > MAX_LOG_SIZE) {
                messageLogs.remove(0);
            }
        }
    }
    
    /**
     * MQ日志条目
     */
    public static class MQLogEntry {
        private String timestamp;
        private String type;
        private String message;
        
        public MQLogEntry(String timestamp, String type, String message) {
            this.timestamp = timestamp;
            this.type = type;
            this.message = message;
        }
        
        // Getters
        public String getTimestamp() { return timestamp; }
        public String getType() { return type; }
        public String getMessage() { return message; }
    }
}
