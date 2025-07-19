package com.example.skillsystem.task;

import com.example.skillsystem.service.StockManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存锁定清理定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockLockCleanupTask {
    
    private final StockManagementService stockManagementService;
    
    /**
     * 每30分钟清理一次过期的库存锁定 - 降低频率
     */
    @Scheduled(fixedRate = 1800000) // 30分钟 = 1800000毫秒
    public void cleanExpiredStockLocks() {
        try {
            log.info("开始执行库存锁定清理任务");
            int cleanedCount = stockManagementService.cleanExpiredLocks();
            log.info("库存锁定清理任务完成，清理了 {} 条过期记录", cleanedCount);
        } catch (Exception e) {
            log.error("库存锁定清理任务执行失败", e);
        }
    }
}
