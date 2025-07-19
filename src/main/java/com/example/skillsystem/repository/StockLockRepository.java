package com.example.skillsystem.repository;

import com.example.skillsystem.entity.StockLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockLockRepository extends JpaRepository<StockLock, Long> {
    
    /**
     * 根据订单号查找库存锁定记录
     */
    Optional<StockLock> findByOrderNo(String orderNo);
    
    /**
     * 根据商品ID和状态查找库存锁定记录
     */
    List<StockLock> findByProductIdAndStatus(Long productId, Integer status);
    
    /**
     * 查找已过期的库存锁定记录
     */
    @Query("SELECT sl FROM StockLock sl WHERE sl.status = 0 AND sl.expireTime < :now")
    List<StockLock> findExpiredLocks(@Param("now") LocalDateTime now);
    
    /**
     * 计算商品的总锁定库存
     */
    @Query("SELECT COALESCE(SUM(sl.lockedQuantity), 0) FROM StockLock sl WHERE sl.productId = :productId AND sl.status = 0")
    Integer getTotalLockedQuantity(@Param("productId") Long productId);

    /**
     * 根据订单号和状态查找库存锁定记录
     */
    List<StockLock> findByOrderNoAndStatus(String orderNo, Integer status);
}
