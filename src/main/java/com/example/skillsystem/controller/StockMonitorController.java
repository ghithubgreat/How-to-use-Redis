package com.example.skillsystem.controller;

import com.example.skillsystem.service.StockSyncService;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class StockMonitorController {

    private final StockSyncService stockSyncService;
    
    /**
     * 库存监控页面
     */
    @GetMapping("/stock")
    public String stockMonitorPage(Model model) {
        Map<String, Object> status = stockSyncService.getStockSyncStatus();
        model.addAttribute("status", status);
        return "monitor/stock";
    }
    
    /**
     * 获取库存同步状态
     */
    @GetMapping("/stock/status")
    @ResponseBody
    public Result<Map<String, Object>> getStockSyncStatus() {
        Map<String, Object> status = stockSyncService.getStockSyncStatus();
        return Result.success(status);
    }
    
    /**
     * 手动同步指定商品库存
     */
    @PostMapping("/stock/sync/{productId}")
    @ResponseBody
    public Result<Void> syncStockByProductId(@PathVariable Long productId) {
        try {
            stockSyncService.syncStockByProductId(productId);
            return Result.success();
        } catch (Exception e) {
            log.error("手动同步库存失败, productId: {}", productId, e);
            return Result.fail("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动全量同步库存
     */
    @PostMapping("/stock/sync/all")
    @ResponseBody
    public Result<Void> syncAllStocks() {
        try {
            stockSyncService.syncAllStocks();
            return Result.success();
        } catch (Exception e) {
            log.error("手动全量同步库存失败", e);
            return Result.fail("同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动处理未同步的库存日志
     */
    @PostMapping("/stock/process-logs")
    @ResponseBody
    public Result<Void> processUnsyncedLogs() {
        try {
            stockSyncService.processUnsyncedLogs();
            return Result.success();
        } catch (Exception e) {
            log.error("手动处理未同步库存日志失败", e);
            return Result.fail("处理失败: " + e.getMessage());
        }
    }
} 