package com.example.skillsystem.enums;

/**
 * 库存操作类型枚举
 */
public enum StockOperationType {
    
    LOCK("LOCK", "锁定库存"),
    DEDUCT("DEDUCT", "扣减库存"),
    ROLLBACK("ROLLBACK", "回滚库存"),
    INCREASE("INCREASE", "增加库存"),
    SYNC("SYNC", "同步库存");
    
    private final String code;
    private final String description;
    
    StockOperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
