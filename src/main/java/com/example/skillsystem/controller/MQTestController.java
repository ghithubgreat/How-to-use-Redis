package com.example.skillsystem.controller;

import com.example.skillsystem.dto.MQStatusDTO;
import com.example.skillsystem.vo.Result;
import com.example.skillsystem.mq.StockMessageProducer;
import com.example.skillsystem.service.MockMQService;
import com.example.skillsystem.service.MQStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MQ测试控制器
 * 提供MQ消息发送测试功能
 */
@Slf4j
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
public class MQTestController {

    private final StockMessageProducer stockMessageProducer;
    private final MockMQService mockMQService;
    private final MQStatisticsService mqStatisticsService;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 测试发送库存扣减消息
     */
    @PostMapping("/test/deduction")
    public Result<String> testDeductionMessage(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String orderNo) {
        
        try {
            if (orderNo == null || orderNo.trim().isEmpty()) {
                orderNo = "TEST-" + System.currentTimeMillis();
            }
            
            stockMessageProducer.sendStockDeductionMessage(productId, quantity, orderNo);
            
            return Result.success("库存扣减消息发送成功: productId=" + productId + 
                                ", quantity=" + quantity + ", orderNo=" + orderNo);
                                
        } catch (Exception e) {
            log.error("发送库存扣减测试消息失败", e);
            return Result.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 测试发送库存回滚消息
     */
    @PostMapping("/test/rollback")
    public Result<String> testRollbackMessage(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String orderNo) {
        
        try {
            if (orderNo == null || orderNo.trim().isEmpty()) {
                orderNo = "TEST-" + System.currentTimeMillis();
            }
            
            stockMessageProducer.sendStockRollbackMessage(productId, quantity, orderNo);
            
            return Result.success("库存回滚消息发送成功: productId=" + productId + 
                                ", quantity=" + quantity + ", orderNo=" + orderNo);
                                
        } catch (Exception e) {
            log.error("发送库存回滚测试消息失败", e);
            return Result.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取MQ状态信息
     */
    @GetMapping("/status")
    public Result<MQStatusDTO> getMQStatus() {
        try {
            String queueStatus = mockMQService.getQueueStatus();

            // 检测当前使用的MQ模式
            String mode = "Mock模式"; // 默认Mock模式
            if (rabbitTemplate != null) {
                try {
                    // 尝试获取RabbitMQ连接信息来检测是否可用
                    rabbitTemplate.execute(channel -> {
                        return channel.getConnection().getServerProperties();
                    });
                    mode = "RabbitMQ模式";
                } catch (Exception e) {
                    log.debug("RabbitMQ不可用，使用Mock模式: {}", e.getMessage());
                    mode = "Mock模式 (RabbitMQ不可用)";
                }
            }

            MQStatusDTO statusDTO = MQStatusDTO.create("运行中", mode, queueStatus);
            return Result.success(statusDTO);

        } catch (Exception e) {
            log.error("获取MQ状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取MQ消息统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getMQStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("deductionCount", mqStatisticsService.getDeductionMessageCount());
            statistics.put("rollbackCount", mqStatisticsService.getRollbackMessageCount());
            statistics.put("logs", mqStatisticsService.getMessageLogs());

            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取MQ统计失败", e);
            return Result.error("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 清空MQ队列和统计
     */
    @PostMapping("/clear")
    public Result<String> clearQueues() {
        try {
            mockMQService.clearAllQueues();
            mqStatisticsService.clearStatistics();
            return Result.success("队列和统计清空成功");
        } catch (Exception e) {
            log.error("清空队列失败", e);
            return Result.error("清空队列失败: " + e.getMessage());
        }
    }
}
