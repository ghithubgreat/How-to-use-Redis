package com.example.skillsystem.enums;

/**
 * 库存锁定状态枚举
 */
public enum StockLockStatus {
    
    LOCKED(0, "锁定中"),
    RELEASED(1, "已释放"),
    DEDUCTED(2, "已扣减");
    
    private final Integer code;
    private final String description;
    
    StockLockStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static StockLockStatus fromCode(Integer code) {
        for (StockLockStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown stock lock status code: " + code);
    }
}
