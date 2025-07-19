package com.example.skillsystem.controller;

import com.example.skillsystem.entity.StockLog;
import com.example.skillsystem.repository.StockLogRepository;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-logs")
@RequiredArgsConstructor
public class StockLogController {

    private final StockLogRepository stockLogRepository;

    /**
     * 分页查询库存流水
     */
    @GetMapping
    public Result<Page<StockLog>> getStockLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String orderId) {
        
        try {
            // 创建分页对象，按创建时间倒序
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            
            // 构建查询条件
            Specification<StockLog> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                if (productId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("productId"), productId));
                }
                
                if (operationType != null && !operationType.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("operationType"), operationType));
                }
                
                if (orderId != null && !orderId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.like(root.get("orderId"), "%" + orderId + "%"));
                }
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            Page<StockLog> stockLogs = stockLogRepository.findAll(spec, pageable);
            return Result.success(stockLogs);
            
        } catch (Exception e) {
            return Result.error("查询库存流水失败: " + e.getMessage());
        }
    }

    /**
     * 获取库存流水统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics() {
        try {
            Map<String, Long> statistics = new HashMap<>();
            
            // 总记录数
            long total = stockLogRepository.count();
            statistics.put("total", total);
            
            // 各类型操作统计
            long deductCount = stockLogRepository.countByOperationType("DEDUCT");
            long increaseCount = stockLogRepository.countByOperationType("INCREASE");
            long rollbackCount = stockLogRepository.countByOperationType("ROLLBACK");
            long syncCount = stockLogRepository.countByOperationType("SYNC");
            
            statistics.put("deduct", deductCount);
            statistics.put("increase", increaseCount);
            statistics.put("rollback", rollbackCount);
            statistics.put("sync", syncCount);
            
            return Result.success(statistics);
            
        } catch (Exception e) {
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据商品ID获取库存流水
     */
    @GetMapping("/product/{productId}")
    public Result<List<StockLog>> getStockLogsByProductId(@PathVariable Long productId) {
        try {
            List<StockLog> stockLogs = stockLogRepository.findByProductIdOrderByCreateTimeDesc(productId);
            return Result.success(stockLogs);
        } catch (Exception e) {
            return Result.error("查询商品库存流水失败: " + e.getMessage());
        }
    }

    /**
     * 根据订单号获取库存流水
     */
    @GetMapping("/order/{orderNo}")
    public Result<List<StockLog>> getStockLogsByOrderNo(@PathVariable String orderNo) {
        try {
            List<StockLog> stockLogs = stockLogRepository.findByOrderIdOrderByCreateTimeDesc(orderNo);
            return Result.success(stockLogs);
        } catch (Exception e) {
            return Result.error("查询订单库存流水失败: " + e.getMessage());
        }
    }
}
