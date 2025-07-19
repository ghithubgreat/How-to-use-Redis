package com.example.skillsystem.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long productId;
    private Integer quantity;
} 