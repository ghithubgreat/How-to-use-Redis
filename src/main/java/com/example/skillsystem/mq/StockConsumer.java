package com.example.skillsystem.mq;

import com.example.skillsystem.constants.MQConstants;
import com.example.skillsystem.repository.ProductRepository;
import com.example.skillsystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockConsumer {

    private final ProductRepository productRepository;
    private final ProductService productService;
    
    /**
     * 处理库存扣减消息
     */
    @RabbitListener(queues = MQConstants.STOCK_DEDUCTION_QUEUE)
    @Transactional
    public void handleStockDeduction(StockMessage message) {
        log.info("接收到库存扣减消息: {}", message);
        
        try {
            // 更新数据库库存
            int rows = productRepository.deductStock(message.getProductId(), message.getQuantity());
            if (rows > 0) {
                log.info("数据库库存扣减成功, productId: {}, quantity: {}", message.getProductId(), message.getQuantity());
            } else {
                log.warn("数据库库存扣减失败, productId: {}, quantity: {}", message.getProductId(), message.getQuantity());
            }
        } catch (Exception e) {
            log.error("处理库存扣减消息异常", e);
        }
    }
    
    /**
     * 处理库存回滚消息
     */
    @RabbitListener(queues = MQConstants.STOCK_ROLLBACK_QUEUE)
    @Transactional
    public void handleStockRollback(StockMessage message) {
        if (!Boolean.TRUE.equals(message.getIsRollback())) {
            log.warn("收到非回滚消息，忽略处理: {}", message);
            return;
        }

        log.info("接收到库存回滚消息: {}", message);

        try {
            // 使用ProductService的increaseStock方法（带订单号），这样会自动记录StockLog
            boolean success = productService.increaseStock(message.getProductId(), message.getQuantity(), message.getOrderNo());
            if (success) {
                log.info("库存回滚成功, productId: {}, quantity: {}, orderNo: {}",
                        message.getProductId(), message.getQuantity(), message.getOrderNo());
            } else {
                log.warn("库存回滚失败, productId: {}, quantity: {}, orderNo: {}",
                        message.getProductId(), message.getQuantity(), message.getOrderNo());
            }
        } catch (Exception e) {
            log.error("处理库存回滚消息异常", e);
        }
    }
} 