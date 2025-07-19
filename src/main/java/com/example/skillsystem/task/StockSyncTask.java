package com.example.skillsystem.task;

import com.example.skillsystem.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncTask {
    
    private final StockSyncService stockSyncService;
    
    /**
     * 每天凌晨2点执行全量同步
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullSyncTask() {
        log.info("开始执行全量库存同步定时任务");
        try {
            stockSyncService.syncAllStocks();
            log.info("全量库存同步定时任务执行完成");
        } catch (Exception e) {
            log.error("全量库存同步定时任务执行异常", e);
        }
    }
    
    /**
     * 每5分钟处理一次未同步的库存日志
     */
    @Scheduled(fixedRate = 300000)
    public void processUnsyncedLogsTask() {
        log.info("开始执行未同步库存日志处理定时任务");
        try {
            stockSyncService.processUnsyncedLogs();
            log.info("未同步库存日志处理定时任务执行完成");
        } catch (Exception e) {
            log.error("未同步库存日志处理定时任务执行异常", e);
        }
    }
} 