package com.example.skillsystem.repository;

import com.example.skillsystem.entity.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Long>, JpaSpecificationExecutor<StockLog> {

    List<StockLog> findByProductIdAndSyncedFalseOrderByCreateTimeAsc(Long productId);
    
    List<StockLog> findBySyncedFalseOrderByCreateTimeAsc();
    
    @Query("SELECT DISTINCT sl.productId FROM StockLog sl WHERE sl.synced = false")
    List<Long> findDistinctProductIdsWithUnsyncedLogs();
    
    List<StockLog> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    // 按操作类型统计
    long countByOperationType(String operationType);

    // 按商品ID查询，按时间倒序
    List<StockLog> findByProductIdOrderByCreateTimeDesc(Long productId);

    // 按订单号查询，按时间倒序
    List<StockLog> findByOrderIdOrderByCreateTimeDesc(String orderId);
} 