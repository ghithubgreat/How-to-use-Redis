package com.example.skillsystem.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer beforeStock;

    @Column(nullable = false)
    private Integer afterStock;

    @Column(nullable = false)
    private Integer changeAmount;

    @Column(length = 20, nullable = false)
    private String operationType;  // DEDUCT, INCREASE, SYNC

    @Column(length = 50)
    private String orderId;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column
    private Boolean synced;  // 是否已同步到Redis
    
    @Column(length = 255)
    private String remark;
} 