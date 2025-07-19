package com.example.skillsystem.controller;

import com.example.skillsystem.dto.ProductDTO;
import com.example.skillsystem.service.ProductService;
import com.example.skillsystem.service.StockManagementService;
import com.example.skillsystem.service.MockRedisService;
import com.example.skillsystem.service.impl.ProductServiceImpl;
import com.example.skillsystem.constants.RedisKeyPrefix;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    private final StockManagementService stockManagementService;
    private final MockRedisService mockRedisService;
    
    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductDTO> getProduct(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        if (product != null) {
            return Result.success(product);
        }
        return Result.error("商品不存在");
    }
    
    /**
     * 获取所有上架商品
     */
    @GetMapping
    public Result<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return Result.success(products);
    }
    
    /**
     * 创建商品
     */
    @PostMapping
    public Result<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        return productService.createProduct(productDTO);
    }
    
    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public Result<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        return productService.updateProduct(id, productDTO);
    }
    
    /**
     * 下架商品
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeProduct(@PathVariable Long id) {
        return productService.removeProduct(id);
    }
    
    /**
     * 重新上架商品
     */
    @PostMapping("/{id}/restore")
    public Result<Boolean> restoreProduct(@PathVariable Long id) {
        return productService.restoreProduct(id);
    }
    
    /**
     * 清除商品缓存
     */
    @PostMapping("/{id}/clear-cache")
    public Result<Boolean> clearProductCache(@PathVariable Long id) {
        productService.clearProductCache(id);
        return Result.success(true);
    }

    /**
     * 获取Redis中的库存
     */
    @GetMapping("/{id}/redis-stock")
    public Result<Integer> getRedisStock(@PathVariable Long id) {
        Integer stock = ((ProductServiceImpl) productService).getRedisStock(id);
        return Result.success(stock);
    }



    /**
     * 获取商品的锁定库存
     */
    @GetMapping("/{id}/locked-stock")
    public Result<Integer> getLockedStock(@PathVariable Long id) {
        Integer lockedStock = stockManagementService.getLockedStock(id);
        return Result.success(lockedStock != null ? lockedStock : 0);
    }

    /**
     * 获取Redis中的可用库存
     */
    @GetMapping("/{id}/available-stock")
    public Result<Integer> getAvailableStock(@PathVariable Long id) {
        Integer availableStock = stockManagementService.getAvailableStock(id);
        return Result.success(availableStock != null ? availableStock : 0);
    }

    /**
     * 清理过期的库存锁定
     */
    @PostMapping("/clean-expired-locks")
    public Result<Integer> cleanExpiredLocks() {
        int cleanedCount = stockManagementService.cleanExpiredLocks();
        return Result.success(cleanedCount);
    }

    /**
     * 同步数据库库存到Redis
     */
    @PostMapping("/{id}/sync-to-redis")
    public Result<Boolean> syncStockToRedis(@PathVariable Long id) {
        boolean result = stockManagementService.syncStockToRedis(id);
        return Result.success(result);
    }

    /**
     * 检查商品是否已缓存到Redis
     */
    @GetMapping("/{id}/cache-status")
    public Result<Map<String, Object>> getCacheStatus(@PathVariable Long id) {
        Map<String, Object> status = new HashMap<>();

        try {
            // 检查商品信息缓存
            String productKey = RedisKeyPrefix.PRODUCT_INFO + id;
            boolean productCached = mockRedisService.exists(productKey);

            // 检查库存缓存
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + id;
            boolean stockCached = mockRedisService.exists(stockKey);

            // 获取缓存的库存值
            Object stockValue = mockRedisService.get(stockKey);

            status.put("productCached", productCached);
            status.put("stockCached", stockCached);
            status.put("stockValue", stockValue);
            status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return Result.success(status);

        } catch (Exception e) {
            log.error("检查缓存状态失败: productId={}, error={}", id, e.getMessage());
            status.put("error", e.getMessage());
            return Result.success(status);
        }
    }
}