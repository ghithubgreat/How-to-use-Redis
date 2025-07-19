package com.example.skillsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 模拟Redis服务，用于在没有Redis环境时进行测试
 */
@Slf4j
@Service
public class MockRedisService {
    
    // 模拟Redis存储
    private final ConcurrentHashMap<String, Object> storage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expireTime = new ConcurrentHashMap<>();
    
    /**
     * 设置值
     */
    public void set(String key, Object value) {
        storage.put(key, value);
        log.debug("MockRedis SET: {} = {}", key, value);
    }
    
    /**
     * 设置值并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        storage.put(key, value);
        long expireTimeMs = System.currentTimeMillis() + unit.toMillis(timeout);
        expireTime.put(key, expireTimeMs);
        log.debug("MockRedis SET with TTL: {} = {}, expire at {}", key, value, expireTimeMs);
    }
    
    /**
     * 获取值
     */
    public Object get(String key) {
        // 检查是否过期
        if (isExpired(key)) {
            delete(key);
            return null;
        }
        Object value = storage.get(key);
        log.debug("MockRedis GET: {} = {}", key, value);
        return value;
    }
    
    /**
     * 删除键
     */
    public void delete(String key) {
        storage.remove(key);
        expireTime.remove(key);
        log.debug("MockRedis DELETE: {}", key);
    }
    
    /**
     * 检查键是否存在
     */
    public boolean exists(String key) {
        if (isExpired(key)) {
            delete(key);
            return false;
        }
        return storage.containsKey(key);
    }
    
    /**
     * 原子性减少值
     */
    public Long decrBy(String key, long delta) {
        if (isExpired(key)) {
            delete(key);
            return null;
        }
        
        Object value = storage.get(key);
        if (value == null) {
            return null;
        }
        
        Long currentValue;
        if (value instanceof Integer) {
            currentValue = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            currentValue = (Long) value;
        } else {
            return null;
        }
        
        Long newValue = currentValue - delta;
        storage.put(key, newValue);
        log.debug("MockRedis DECRBY: {} - {} = {}", key, delta, newValue);
        return newValue;
    }
    
    /**
     * 原子性增加值
     */
    public Long incrBy(String key, long delta) {
        if (isExpired(key)) {
            delete(key);
            return null;
        }
        
        Object value = storage.get(key);
        Long currentValue = 0L;
        
        if (value != null) {
            if (value instanceof Integer) {
                currentValue = ((Integer) value).longValue();
            } else if (value instanceof Long) {
                currentValue = (Long) value;
            }
        }
        
        Long newValue = currentValue + delta;
        storage.put(key, newValue);
        log.debug("MockRedis INCRBY: {} + {} = {}", key, delta, newValue);
        return newValue;
    }
    
    /**
     * 检查键是否过期
     */
    private boolean isExpired(String key) {
        Long expireTimeMs = expireTime.get(key);
        if (expireTimeMs == null) {
            return false;
        }
        return System.currentTimeMillis() > expireTimeMs;
    }
    
    /**
     * 获取所有键（用于调试）
     */
    public void printAllKeys() {
        log.info("MockRedis 当前存储的键值对:");
        storage.forEach((key, value) -> {
            Long expireTimeMs = expireTime.get(key);
            String expireInfo = expireTimeMs != null ? 
                (isExpired(key) ? " (已过期)" : " (过期时间: " + expireTimeMs + ")") : " (永不过期)";
            log.info("  {} = {}{}", key, value, expireInfo);
        });
    }
    
    /**
     * 原子扣减操作（模拟Lua脚本）
     * @param key 键
     * @param quantity 扣减数量
     * @return 扣减后的值，如果库存不足返回-1，如果键不存在返回-2
     */
    public synchronized Long decrBy(String key, Integer quantity) {
        // 检查是否过期
        if (isExpired(key)) {
            delete(key);
            log.debug("MockRedis DECRBY: key {} expired, returning -2", key);
            return -2L; // 键不存在
        }

        Object value = storage.get(key);
        if (value == null) {
            log.debug("MockRedis DECRBY: key {} not exists, returning -2", key);
            return -2L; // 键不存在
        }

        try {
            Integer currentValue = Integer.parseInt(value.toString());
            if (currentValue < quantity) {
                log.debug("MockRedis DECRBY: insufficient stock, current={}, required={}, returning -1", currentValue, quantity);
                return -1L; // 库存不足
            }

            Integer newValue = currentValue - quantity;
            storage.put(key, newValue);
            log.debug("MockRedis DECRBY: {} = {} - {} = {}", key, currentValue, quantity, newValue);
            return newValue.longValue();
        } catch (NumberFormatException e) {
            log.error("MockRedis DECRBY: value is not a number, key={}, value={}", key, value);
            return -2L; // 值不是数字
        }
    }

    /**
     * 原子增加操作
     * @param key 键
     * @param quantity 增加数量
     * @return 增加后的值
     */
    public synchronized Long incrBy(String key, Integer quantity) {
        // 检查是否过期
        if (isExpired(key)) {
            delete(key);
        }

        Object value = storage.get(key);
        Integer currentValue = 0;

        if (value != null) {
            try {
                currentValue = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.error("MockRedis INCRBY: value is not a number, key={}, value={}", key, value);
                currentValue = 0;
            }
        }

        Integer newValue = currentValue + quantity;
        storage.put(key, newValue);
        log.debug("MockRedis INCRBY: {} = {} + {} = {}", key, currentValue, quantity, newValue);
        return newValue.longValue();
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        storage.clear();
        expireTime.clear();
        log.info("MockRedis 已清空所有数据");
    }
}
