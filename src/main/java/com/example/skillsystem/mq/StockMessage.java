package com.example.skillsystem.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long productId;
    private Integer quantity;
    private String orderNo;
    private Boolean isRollback = false;
} 