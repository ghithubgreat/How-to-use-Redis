package com.example.skillsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    // 商品相关配置
    private final Product product = new Product();
    
    // 订单相关配置
    private final Order order = new Order();
    
    public static class Product {
        private Integer cacheTtl = 3600;
        
        public Integer getCacheTtl() {
            return cacheTtl;
        }
        
        public void setCacheTtl(Integer cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }
    
    public static class Order {
        private Integer paymentTimeout = 300000;
        
        public Integer getPaymentTimeout() {
            return paymentTimeout;
        }
        
        public void setPaymentTimeout(Integer paymentTimeout) {
            this.paymentTimeout = paymentTimeout;
        }
    }
    
    public Integer getProductCacheTtl() {
        return product.getCacheTtl();
    }
    
    public Integer getOrderPaymentTimeout() {
        return order.getPaymentTimeout();
    }
    
    public Product getProduct() {
        return product;
    }
    
    public Order getOrder() {
        return order;
    }
} 