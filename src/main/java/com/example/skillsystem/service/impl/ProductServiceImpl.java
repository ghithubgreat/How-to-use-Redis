package com.example.skillsystem.service.impl;


import com.example.skillsystem.config.AppConfig;
import com.example.skillsystem.constants.RedisKeyPrefix;
import com.example.skillsystem.dto.ProductDTO;
import com.example.skillsystem.entity.Product;
import com.example.skillsystem.entity.StockLog;
import com.example.skillsystem.repository.ProductRepository;
import com.example.skillsystem.repository.StockLogRepository;
import com.example.skillsystem.service.MockRedisService;
import com.example.skillsystem.service.ProductService;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;


import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final StockLogRepository stockLogRepository;
    private final MockRedisService mockRedisService;
    private final AppConfig appConfig;
    
    @Override
    public ProductDTO getProductById(Long id) {
        log.info("获取商品信息, id: {}", id);

        // 构建缓存键
        String productInfoKey = RedisKeyPrefix.PRODUCT_INFO + id;
        String stockKey = RedisKeyPrefix.PRODUCT_STOCK + id;
        ProductDTO productDTO = null;

        try {
            // 尝试从Redis缓存获取商品信息
            Object productObject = mockRedisService.get(productInfoKey);
            Object stockObject = mockRedisService.get(stockKey);

            if (productObject != null) {
                log.info("从Redis缓存获取商品信息, id: {}", id);
                productDTO = (ProductDTO) productObject;

                // 如果有库存缓存，使用Redis中的库存
                if (stockObject != null) {
                    Integer redisStock = (Integer) stockObject;
                    productDTO.setStock(redisStock);
                    log.info("使用Redis库存: {}", redisStock);
                } else {
                    log.warn("Redis中没有库存缓存，使用商品信息中的库存: {}", productDTO.getStock());
                }

                return productDTO;
            } else {
                log.info("Redis缓存未命中，将从数据库获取商品信息, id: {}", id);
            }
        } catch (Exception e) {
            log.error("Redis缓存读取失败, id: {}, error: {}", id, e.getMessage());
            // Redis异常，继续从数据库获取
        }

        // 缓存未命中或Redis异常，从数据库获取
        log.info("从数据库获取商品信息, id: {}", id);
        Optional<Product> productOpt = productRepository.findById(id);
        if (!productOpt.isPresent()) {
            log.warn("商品不存在, id: {}", id);
            return null;
        }

        Product product = productOpt.get();
        productDTO = new ProductDTO();
        BeanUtils.copyProperties(product, productDTO);

        // 将商品信息和库存写入Redis缓存
        try {
            log.info("将商品信息写入Redis缓存, id: {}", id);

            // 写入商品信息缓存
            mockRedisService.set(productInfoKey, productDTO, appConfig.getProductCacheTtl(), TimeUnit.SECONDS);

            // 写入库存缓存
            mockRedisService.set(stockKey, product.getStock(), appConfig.getProductCacheTtl(), TimeUnit.SECONDS);

            log.info("商品信息已写入Redis缓存: id={}, name={}, status={}, stock={}",
                    productDTO.getId(), productDTO.getName(), productDTO.getStatus(), productDTO.getStock());
        } catch (Exception e) {
            log.error("Redis缓存写入失败, id: {}, error: {}", id, e.getMessage());
            // 缓存写入失败不影响返回结果
        }

        return productDTO;
    }

    /**
     * 获取Redis中的实时库存
     */
    public Integer getRedisStock(Long productId) {
        try {
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
            Object stockObject = mockRedisService.get(stockKey);
            if (stockObject != null) {
                return (Integer) stockObject;
            }
        } catch (Exception e) {
            log.error("获取Redis库存失败, productId: {}, error: {}", productId, e.getMessage());
        }
        return null;
    }

    /**
     * 同步数据库库存到Redis
     */
    public void syncStockToRedis(Long productId) {
        try {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
                mockRedisService.set(stockKey, product.getStock(), appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
                log.info("同步库存到Redis: productId={}, stock={}", productId, product.getStock());
            }
        } catch (Exception e) {
            log.error("同步库存到Redis失败, productId: {}, error: {}", productId, e.getMessage());
        }
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        // 从数据库获取所有商品
        List<Product> products = productRepository.findAll();

        // 转换为DTO列表，优先使用Redis中的库存信息
        return products.stream()
                .map(product -> {
                    ProductDTO dto = new ProductDTO();
                    BeanUtils.copyProperties(product, dto);

                    // 尝试从Redis获取实时库存
                    String stockKey = RedisKeyPrefix.PRODUCT_STOCK + product.getId();
                    Object redisStock = mockRedisService.get(stockKey);

                    if (redisStock != null) {
                        // 使用Redis中的库存
                        try {
                            dto.setStock(Integer.parseInt(redisStock.toString()));
                            log.debug("商品列表使用Redis库存: productId={}, stock={}", product.getId(), redisStock);
                        } catch (NumberFormatException e) {
                            log.warn("Redis库存格式错误, productId={}, redisStock={}, 使用数据库库存",
                                    product.getId(), redisStock);
                            dto.setStock(product.getStock());
                        }
                    } else {
                        // Redis中没有库存信息，使用数据库库存
                        dto.setStock(product.getStock());
                        log.debug("商品列表使用数据库库存: productId={}, stock={}", product.getId(), product.getStock());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDTO> getProductsByStatus(Integer status) {
        // 从数据库获取指定状态的商品
        List<Product> products = productRepository.findByStatus(status);
        
        // 转换为DTO列表
        return products.stream()
                .map(product -> {
                    ProductDTO dto = new ProductDTO();
                    BeanUtils.copyProperties(product, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Result<ProductDTO> createProduct(ProductDTO productDTO) {
        // 参数校验
        if (productDTO == null || !StringUtils.hasText(productDTO.getName()) || productDTO.getPrice() == null) {
            return Result.error("商品信息不完整");
        }
        
        try {
            // 创建商品实体
            Product product = new Product();
            BeanUtils.copyProperties(productDTO, product);
            
            // 设置默认值
            product.setCreateTime(LocalDateTime.now());
            product.setUpdateTime(LocalDateTime.now());
            product.setStatus(1); // 默认上架
            
            // 保存到数据库
            product = productRepository.save(product);
            
            // 转换为DTO
            ProductDTO savedDTO = new ProductDTO();
            BeanUtils.copyProperties(product, savedDTO);
            
            // 尝试写入Redis缓存
            try {
                String productInfoKey = RedisKeyPrefix.PRODUCT_INFO + product.getId();
                mockRedisService.set(productInfoKey, savedDTO, appConfig.getProductCacheTtl(), TimeUnit.SECONDS);

                // 将库存写入缓存
                String stockKey = RedisKeyPrefix.PRODUCT_STOCK + product.getId();
                mockRedisService.set(stockKey, product.getStock(), appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("创建商品时Redis缓存写入失败, id: {}, error: {}", product.getId(), e.getMessage());
                // 缓存写入失败不影响正常业务
            }
            
            log.info("商品创建成功, id: {}, name: {}", product.getId(), product.getName());
            
            return Result.success(savedDTO);
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return Result.error("创建商品失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result<ProductDTO> updateProduct(Long id, ProductDTO productDTO) {
        // 参数校验
        if (id == null || productDTO == null) {
            return Result.error("参数不正确");
        }
        
        try {
            // 查询商品
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) {
                return Result.error("商品不存在");
            }
            
            Product product = productOpt.get();
            
            // 更新商品信息
            if (StringUtils.hasText(productDTO.getName())) {
                product.setName(productDTO.getName());
            }
            
            if (StringUtils.hasText(productDTO.getDescription())) {
                product.setDescription(productDTO.getDescription());
            }
            
            if (productDTO.getPrice() != null) {
                product.setPrice(productDTO.getPrice());
            }
            
            if (productDTO.getStock() != null) {
                product.setStock(productDTO.getStock());
            }
            
            if (StringUtils.hasText(productDTO.getImageUrl())) {
                product.setImageUrl(productDTO.getImageUrl());
            }
            
            product.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            product = productRepository.save(product);
            
            // 转换为DTO
            ProductDTO updatedDTO = new ProductDTO();
            BeanUtils.copyProperties(product, updatedDTO);
            
            // 尝试清除Redis缓存
            try {
                clearProductCache(id);
            } catch (Exception e) {
                log.error("更新商品时清除Redis缓存失败, id: {}, error: {}", id, e.getMessage());
                // 缓存清除失败不影响正常业务
            }
            
            log.info("商品更新成功, id: {}, name: {}", product.getId(), product.getName());
            
            return Result.success(updatedDTO);
        } catch (Exception e) {
            log.error("更新商品失败", e);
            return Result.error("更新商品失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result<Boolean> removeProduct(Long id) {
        // 参数校验
        if (id == null) {
            return Result.error("商品ID不能为空");
        }
        
        try {
            // 查询商品
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) {
                return Result.error("商品不存在");
            }
            
            Product product = productOpt.get();
            
            // 更新商品状态为下架
            product.setStatus(0);
            product.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            productRepository.save(product);
            
            // 清除Redis缓存
            clearProductCache(id);
            
            log.info("商品下架成功, id: {}, name: {}", product.getId(), product.getName());
            
            return Result.success(true);
        } catch (Exception e) {
            log.error("下架商品失败", e);
            return Result.error("下架商品失败: " + e.getMessage());
        }
    }
    
    @Override
    public void clearProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        
        try {
            // 清除商品信息缓存
            String productInfoKey = RedisKeyPrefix.PRODUCT_INFO + productId;
            mockRedisService.delete(productInfoKey);

            // 清除库存缓存
            String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
            mockRedisService.delete(stockKey);
            
            log.info("清除商品缓存成功, productId: {}", productId);
        } catch (Exception e) {
            log.error("清除商品缓存失败, productId: {}, error: {}", productId, e.getMessage());
            // 缓存清除失败不影响正常业务
        }
    }
    
    @Override
    @Transactional
    public boolean deductStock(Long productId, Integer quantity) {
        return deductStock(productId, quantity, null);
    }

    @Override
    @Transactional
    public boolean deductStock(Long productId, Integer quantity, String orderNo) {
        // 先扣减Redis中的库存（使用原子操作保证原子性）
        String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
        Long result = mockRedisService.decrBy(stockKey, quantity);
        
        // 判断扣减结果
        if (result == null) {
            log.error("扣减Redis库存失败, productId: {}, quantity: {}", productId, quantity);
            return false;
        }
        
        if (result < 0) {
            // 处理不同的错误码
            if (result == -1L) {
                log.warn("Redis中不存在该商品库存, productId: {}, 从数据库加载", productId);
                // Redis中不存在库存，从数据库加载
                Optional<Product> productOpt = productRepository.findById(productId);
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    if (product.getStock() >= quantity) {
                        // 将库存写入Redis
                        mockRedisService.set(stockKey, product.getStock(), appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
                        // 重新执行扣减
                        return deductStock(productId, quantity, orderNo);
                    } else {
                        log.warn("库存不足, productId: {}, stock: {}, quantity: {}", productId, product.getStock(), quantity);
                        return false;
                    }
                } else {
                    log.error("商品不存在, productId: {}", productId);
                    return false;
                }
            } else if (result == -2L) {
                log.warn("库存不足, productId: {}, quantity: {}", productId, quantity);
                return false;
            } else {
                log.error("未知的Redis返回结果: {}", result);
                return false;
            }
        }
        
        log.info("Redis库存扣减成功, productId: {}, quantity: {}, 剩余库存: {}", productId, quantity, result);
        
        // 记录库存扣减日志
        StockLog stockLog = StockLog.builder()
                .productId(productId)
                .beforeStock(result.intValue() + quantity)
                .afterStock(result.intValue())
                .changeAmount(quantity)
                .operationType("DEDUCT")
                .orderId(orderNo)
                .createTime(LocalDateTime.now())
                .synced(true)
                .build();
        stockLogRepository.save(stockLog);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean increaseStock(Long productId, Integer quantity) {
        return increaseStock(productId, quantity, null);
    }

    @Override
    @Transactional
    public boolean increaseStock(Long productId, Integer quantity, String orderNo) {
        // 增加Redis中的库存
        String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
        boolean hasKey = mockRedisService.exists(stockKey);
        Integer beforeStock = 0;
        Integer afterStock = 0;

        if (hasKey) {
            Object stockObj = mockRedisService.get(stockKey);
            if (stockObj != null) {
                beforeStock = Integer.parseInt(stockObj.toString());
            }

            Long newStock = mockRedisService.incrBy(stockKey, quantity);
            afterStock = newStock.intValue();
            log.info("Redis库存增加成功, productId: {}, quantity: {}", productId, quantity);
        } else {
            // 如果Redis中不存在该商品库存，从数据库加载
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                beforeStock = product.getStock();
                mockRedisService.set(stockKey, product.getStock(), appConfig.getProductCacheTtl(), TimeUnit.SECONDS);
                log.info("从数据库加载库存到Redis, productId: {}, stock: {}", productId, product.getStock());
                afterStock = product.getStock();
            } else {
                log.error("商品不存在, productId: {}", productId);
                return false;
            }
        }
        
        // 数据库中增加库存
        int rows = productRepository.increaseStock(productId, quantity);
        
        // 记录库存增加日志
        StockLog stockLog = StockLog.builder()
                .productId(productId)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .changeAmount(quantity)
                .operationType("INCREASE")
                .orderId(orderNo)
                .createTime(LocalDateTime.now())
                .synced(true)
                .build();
        stockLogRepository.save(stockLog);
        
        return rows > 0;
    }
    
    @Override
    @Transactional
    public Result<Boolean> restoreProduct(Long id) {
        // 参数校验
        if (id == null) {
            return Result.error("商品ID不能为空");
        }
        
        try {
            // 查询商品
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) {
                return Result.error("商品不存在");
            }
            
            Product product = productOpt.get();
            
            // 更新商品状态为上架
            product.setStatus(1);
            product.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            productRepository.save(product);
            
            // 清除Redis缓存
            clearProductCache(id);
            
            log.info("商品重新上架成功, id: {}, name: {}", product.getId(), product.getName());
            
            return Result.success(true);
        } catch (Exception e) {
            log.error("重新上架商品失败", e);
            return Result.error("重新上架商品失败: " + e.getMessage());
        }
    }
} 