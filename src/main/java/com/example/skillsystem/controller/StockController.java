package com.example.skillsystem.controller;

import com.example.skillsystem.service.StockSyncService;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {
    
    private final StockSyncService stockSyncService;
    
    @PostMapping("/sync")
    public Result<Void> syncStock() {
        try {
            stockSyncService.syncAllStocks();
            return Result.success();
        } catch (Exception e) {
            return Result.error("同步库存失败：" + e.getMessage());
        }
    }
} 