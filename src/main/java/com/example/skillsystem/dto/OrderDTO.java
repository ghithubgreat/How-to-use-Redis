package com.example.skillsystem.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime paymentTime;
    private LocalDateTime expireTime;
    private LocalDateTime updateTime;
} 