package com.example.skillsystem.config;

import com.example.skillsystem.service.MockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动配置
 * 在应用启动时执行初始化操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfig implements ApplicationRunner {
    
    private final MockRedisService mockRedisService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("应用启动初始化开始...");
        
        // 清空MockRedis缓存，确保每次启动都是干净的状态
        mockRedisService.clear();
        log.info("MockRedis缓存已清空");
        
        log.info("应用启动初始化完成");
    }
}
