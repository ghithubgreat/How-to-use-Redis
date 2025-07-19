package com.example.skillsystem.repository;

import com.example.skillsystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * 根据订单号查询订单
     * 
     * @param orderNo 订单号
     * @return 订单对象
     */
    Optional<Order> findByOrderNo(String orderNo);
    
    /**
     * 根据订单状态查询订单
     * 
     * @param status 订单状态
     * @return 订单列表
     */
    List<Order> findByStatus(Integer status);
    
    /**
     * 更新订单为已支付状态
     * 
     * @param orderNo 订单号
     * @param status 订单状态
     * @param paymentTime 支付时间
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.paymentTime = :paymentTime, o.updateTime = CURRENT_TIMESTAMP WHERE o.orderNo = :orderNo AND o.status = 0")
    int updateOrderPaid(String orderNo, Integer status, LocalDateTime paymentTime);
    
    /**
     * 更新订单为取消状态
     * 
     * @param orderNo 订单号
     * @param status 订单状态
     * @param updateTime 取消时间
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.updateTime = :updateTime WHERE o.orderNo = :orderNo AND o.status = 0")
    int updateOrderCancelled(String orderNo, Integer status, LocalDateTime updateTime);
    
    /**
     * 查询已过期未支付的订单
     *
     * @param status 订单状态
     * @param expireTime 过期时间
     * @return 订单列表
     */
    List<Order> findByStatusAndExpireTimeLessThan(Integer status, LocalDateTime expireTime);

    /**
     * 查找指定状态且创建时间小于指定时间的订单
     *
     * @param status 订单状态
     * @param createTime 创建时间
     * @return 订单列表
     */
    List<Order> findByStatusAndCreateTimeLessThan(Integer status, LocalDateTime createTime);
} 