package com.example.skillsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/monitor")
public class StockMonitorViewController {

    @GetMapping("/stock-log")
    public String stockLogPage() {
        return "monitor/stock-log";
    }

    @GetMapping("/redis-cache")
    public String redisCacheMonitor() {
        return "monitor/redis-cache";
    }

    @GetMapping("/stock-status")
    public String stockStatusMonitor() {
        return "monitor/stock-status";
    }
}
