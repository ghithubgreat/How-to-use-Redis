package com.example.skillsystem.mq;

import com.example.skillsystem.constants.MQConstants;
import com.example.skillsystem.service.MockMQService;
import com.example.skillsystem.service.MQStatisticsService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 库存消息生产者
 * 负责发送库存相关的MQ消息
 */
@Slf4j
@Service
public class StockMessageProducer {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MockMQService mockMQService;

    @Autowired
    private MQStatisticsService mqStatisticsService;

    /**
     * 发送库存扣减消息
     * 用于异步扣减数据库库存
     */
    public void sendStockDeductionMessage(Long productId, Integer quantity, String orderNo) {
        StockMessage message = new StockMessage(productId, quantity, orderNo, false);

        if (rabbitTemplate != null) {
            // 使用真实的RabbitMQ
            try {
                rabbitTemplate.convertAndSend(
                    MQConstants.STOCK_DEDUCTION_EXCHANGE,
                    MQConstants.STOCK_DEDUCTION_ROUTING_KEY,
                    message
                );

                log.info("🐰 [RabbitMQ] 发送库存扣减消息成功: productId={}, quantity={}, orderNo={}",
                        productId, quantity, orderNo);

                // 记录消息统计
                mqStatisticsService.recordDeductionMessage(productId, quantity, orderNo, true);

            } catch (Exception e) {
                log.error("🐰 [RabbitMQ] 发送库存扣减消息失败，切换到Mock模式: productId={}, quantity={}, orderNo={}, error={}",
                        productId, quantity, orderNo, e.getMessage());
                // 记录失败统计
                mqStatisticsService.recordDeductionMessage(productId, quantity, orderNo, false);
                // 如果RabbitMQ失败，降级到Mock模式
                mockMQService.sendStockDeductionMessage(message);
            }
        } else {
            // 使用Mock MQ服务
            log.info("🔧 [Mock模式] RabbitMQ不可用，使用Mock MQ服务");
            mockMQService.sendStockDeductionMessage(message);
            // 记录Mock模式统计
            mqStatisticsService.recordDeductionMessage(productId, quantity, orderNo, true);
        }
    }

    /**
     * 发送库存回滚消息
     * 用于异步回滚库存
     */
    public void sendStockRollbackMessage(Long productId, Integer quantity, String orderNo) {
        StockMessage message = new StockMessage(productId, quantity, orderNo, true);

        if (rabbitTemplate != null) {
            // 使用真实的RabbitMQ
            try {
                rabbitTemplate.convertAndSend(
                    MQConstants.STOCK_ROLLBACK_EXCHANGE,
                    MQConstants.STOCK_ROLLBACK_ROUTING_KEY,
                    message
                );

                log.info("🐰 [RabbitMQ] 发送库存回滚消息成功: productId={}, quantity={}, orderNo={}",
                        productId, quantity, orderNo);

                // 记录消息统计
                mqStatisticsService.recordRollbackMessage(productId, quantity, orderNo, true);

            } catch (Exception e) {
                log.error("🐰 [RabbitMQ] 发送库存回滚消息失败，切换到Mock模式: productId={}, quantity={}, orderNo={}, error={}",
                        productId, quantity, orderNo, e.getMessage());
                // 记录失败统计
                mqStatisticsService.recordRollbackMessage(productId, quantity, orderNo, false);
                // 如果RabbitMQ失败，降级到Mock模式
                mockMQService.sendStockRollbackMessage(message);
            }
        } else {
            // 使用Mock MQ服务
            log.info("🔧 [Mock模式] RabbitMQ不可用，使用Mock MQ服务");
            mockMQService.sendStockRollbackMessage(message);
            // 记录Mock模式统计
            mqStatisticsService.recordRollbackMessage(productId, quantity, orderNo, true);
        }
    }

    /**
     * 发送库存同步消息
     * 用于触发库存同步操作
     */
    public void sendStockSyncMessage(Long productId) {
        try {
            StockMessage message = new StockMessage(productId, 0, null, false);
            
            // 可以复用扣减队列，数量为0表示同步操作
            rabbitTemplate.convertAndSend(
                MQConstants.STOCK_DEDUCTION_EXCHANGE,
                MQConstants.STOCK_DEDUCTION_ROUTING_KEY,
                message
            );
            
            log.info("发送库存同步消息成功: productId={}", productId);
                    
        } catch (Exception e) {
            log.error("发送库存同步消息失败: productId={}, error={}", productId, e.getMessage(), e);
        }
    }
}
