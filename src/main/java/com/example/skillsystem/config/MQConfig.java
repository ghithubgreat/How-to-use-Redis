package com.example.skillsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * MQ配置类
 * 处理RabbitMQ可用和不可用的情况
 */
@Slf4j
@Configuration
public class MQConfig {

    /**
     * 检查RabbitMQ是否可用
     */
    @Bean
    @ConditionalOnClass(ConnectionFactory.class)
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public String rabbitMQStatus() {
        log.info("🐰 RabbitMQ配置已启用，尝试连接到RabbitMQ服务器");
        return "enabled";
    }

    /**
     * 当RabbitMQ不可用时的配置
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host", matchIfMissing = true, havingValue = "")
    public String mockMQStatus() {
        log.warn("🔧 RabbitMQ配置未启用或不可用，将使用Mock MQ服务");
        return "mock";
    }
}
