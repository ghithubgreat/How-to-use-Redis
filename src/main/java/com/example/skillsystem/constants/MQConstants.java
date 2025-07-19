package com.example.skillsystem.constants;

public class MQConstants {
    
    /**
     * 库存扣减队列
     */
    public static final String STOCK_DEDUCTION_QUEUE = "stock.deduction.queue";
    
    /**
     * 库存扣减交换机
     */
    public static final String STOCK_DEDUCTION_EXCHANGE = "stock.deduction.exchange";
    
    /**
     * 库存扣减路由键
     */
    public static final String STOCK_DEDUCTION_ROUTING_KEY = "stock.deduction";
    
    /**
     * 库存回滚队列
     */
    public static final String STOCK_ROLLBACK_QUEUE = "stock.rollback.queue";
    
    /**
     * 库存回滚交换机
     */
    public static final String STOCK_ROLLBACK_EXCHANGE = "stock.rollback.exchange";
    
    /**
     * 库存回滚路由键
     */
    public static final String STOCK_ROLLBACK_ROUTING_KEY = "stock.rollback";
} 