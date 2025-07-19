package com.example.skillsystem.config;

import com.example.skillsystem.constants.MQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    /**
     * 库存扣减队列
     */
    @Bean
    public Queue stockDeductionQueue() {
        return new Queue(MQConstants.STOCK_DEDUCTION_QUEUE, true);
    }
    
    /**
     * 库存扣减交换机
     */
    @Bean
    public DirectExchange stockDeductionExchange() {
        return new DirectExchange(MQConstants.STOCK_DEDUCTION_EXCHANGE, true, false);
    }
    
    /**
     * 库存扣减绑定
     */
    @Bean
    public Binding stockDeductionBinding() {
        return BindingBuilder.bind(stockDeductionQueue())
                .to(stockDeductionExchange())
                .with(MQConstants.STOCK_DEDUCTION_ROUTING_KEY);
    }
    
    /**
     * 库存回滚队列
     */
    @Bean
    public Queue stockRollbackQueue() {
        return new Queue(MQConstants.STOCK_ROLLBACK_QUEUE, true);
    }
    
    /**
     * 库存回滚交换机
     */
    @Bean
    public DirectExchange stockRollbackExchange() {
        return new DirectExchange(MQConstants.STOCK_ROLLBACK_EXCHANGE, true, false);
    }
    
    /**
     * 库存回滚绑定
     */
    @Bean
    public Binding stockRollbackBinding() {
        return BindingBuilder.bind(stockRollbackQueue())
                .to(stockRollbackExchange())
                .with(MQConstants.STOCK_ROLLBACK_ROUTING_KEY);
    }
    
    /**
     * 消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
} 