package com.example.skillsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ状态信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MQStatusDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * MQ运行状态
     */
    private String status;
    
    /**
     * MQ模式（RabbitMQ/Mock）
     */
    private String mode;
    
    /**
     * 队列信息
     */
    private String queueInfo;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 创建MQ状态信息
     */
    public static MQStatusDTO create(String status, String mode, String queueInfo) {
        return new MQStatusDTO(status, mode, queueInfo, System.currentTimeMillis());
    }
}
