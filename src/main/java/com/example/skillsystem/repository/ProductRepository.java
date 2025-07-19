package com.example.skillsystem.repository;

import com.example.skillsystem.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * 根据状态查询商品
     * 
     * @param status 商品状态
     * @return 商品列表
     */
    List<Product> findByStatus(Integer status);
    
    /**
     * 扣减库存
     * 
     * @param id 商品ID
     * @param quantity 扣减数量
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity, p.updateTime = CURRENT_TIMESTAMP WHERE p.id = :id AND p.stock >= :quantity")
    int deductStock(Long id, Integer quantity);
    
    /**
     * 增加库存
     * 
     * @param id 商品ID
     * @param quantity 增加数量
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.updateTime = CURRENT_TIMESTAMP WHERE p.id = :id")
    int increaseStock(Long id, Integer quantity);
} 