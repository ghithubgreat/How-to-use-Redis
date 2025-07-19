package com.example.skillsystem.constants;

public class RedisKeyPrefix {
    
    /**
     * 商品信息缓存前缀
     */
    public static final String PRODUCT_INFO = "product:info:";
    
    /**
     * 商品库存缓存前缀
     */
    public static final String PRODUCT_STOCK = "product:stock:";
    
    /**
     * 订单信息缓存前缀
     */
    public static final String ORDER_INFO = "order:info:";
    
    /**
     * 订单支付状态前缀
     */
    public static final String ORDER_PAYMENT = "order:payment:";
} 