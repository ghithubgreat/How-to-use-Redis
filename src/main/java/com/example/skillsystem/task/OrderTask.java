package com.example.skillsystem.task;

import com.example.skillsystem.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTask {
    
    private final OrderService orderService;
    
    /**
     * 每10分钟检查一次超时未支付订单
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    public void checkTimeoutOrders() {
        log.info("定时任务 - 开始检查超时未支付订单");
        orderService.handleTimeoutOrders();
    }
} 