package com.example.skillsystem.service.impl;

import com.example.skillsystem.config.AppConfig;

import com.example.skillsystem.constants.RedisKeyPrefix;
import com.example.skillsystem.dto.OrderDTO;
import com.example.skillsystem.dto.ProductDTO;
import com.example.skillsystem.entity.Order;
import com.example.skillsystem.enums.OrderStatus;

import com.example.skillsystem.repository.OrderRepository;
import com.example.skillsystem.service.OrderService;
import com.example.skillsystem.service.ProductService;

import com.example.skillsystem.service.StockManagementService;
import com.example.skillsystem.vo.OrderRequest;
import com.example.skillsystem.vo.PaymentRequest;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final StockManagementService stockManagementService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppConfig appConfig;
    
    @Override
    @Transactional
    public Result<OrderDTO> createOrder(OrderRequest request) {
        // 检查请求参数
        if (request.getProductId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            return Result.error("请求参数不正确");
        }
        
        // 获取商品信息
        ProductDTO product = productService.getProductById(request.getProductId());
        if (product == null) {
            return Result.error("商品不存在");
        }
        
        try {
            // 先生成订单号
            String orderNo = UUID.randomUUID().toString().replace("-", "");

            // 下单扣Redis：快速锁定库存，保障并发性能
            boolean lockResult = stockManagementService.lockRedisStock(request.getProductId(), orderNo, request.getQuantity());
            if (!lockResult) {
                return Result.error("库存不足，无法锁定");
            }

            // 创建订单
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setProductId(request.getProductId());
            order.setProductName(product.getName());
            order.setProductPrice(product.getPrice());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(product.getPrice().multiply(new BigDecimal(request.getQuantity())));
            order.setStatus(OrderStatus.WAITING_PAYMENT.getCode());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            // 设置超时时间，根据配置的超时毫秒数计算
            order.setExpireTime(LocalDateTime.now().plusNanos(appConfig.getOrderPaymentTimeout() * 1000000L));
            
            // 保存订单到数据库
            order = orderRepository.save(order);
            
            // 新的库存管理策略：下单时已经锁定了Redis库存，无需发送MQ消息
            log.info("订单创建成功，已锁定Redis库存: orderNo={}, productId={}, quantity={}",
                    order.getOrderNo(), request.getProductId(), request.getQuantity());
            
            // 注意：暂时不将订单对象存入Redis，避免LocalDateTime序列化问题
            // 如果需要缓存，可以考虑只存储订单号和关键信息
            log.info("订单已保存到数据库: orderNo={}", order.getOrderNo());
            
            // 转换为DTO返回
            OrderDTO orderDTO = new OrderDTO();
            BeanUtils.copyProperties(order, orderDTO);
            
            return Result.success(orderDTO);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            // 回滚Redis库存
            productService.increaseStock(request.getProductId(), request.getQuantity());
            return Result.error("创建订单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result<Boolean> payOrder(PaymentRequest request) {
        // 检查请求参数
        if (request.getOrderNo() == null) {
            return Result.error("请求参数不正确");
        }
        
        // 查询订单
        Optional<Order> orderOpt = orderRepository.findByOrderNo(request.getOrderNo());
        if (orderOpt.isEmpty()) {
            return Result.error("订单不存在");
        }
        
        Order order = orderOpt.get();
        
        // 检查订单状态
        if (!OrderStatus.WAITING_PAYMENT.getCode().equals(order.getStatus())) {
            return Result.error("订单状态不正确，无法支付");
        }
        
        // 检查订单是否过期
        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
            return Result.error("订单已过期，无法支付");
        }
        
        try {
            // 支付扣数据库：最终一致性落地，Redis无需重复扣减
            boolean deductResult = stockManagementService.deductDatabaseStock(request.getOrderNo());
            if (!deductResult) {
                return Result.error("库存扣减失败，支付失败");
            }

            // 更新订单状态为已支付
            int rows = orderRepository.updateOrderPaid(
                    request.getOrderNo(),
                    OrderStatus.PAID.getCode(),
                    LocalDateTime.now()
            );

            if (rows > 0) {
                // 支付成功，从Redis中删除订单信息
                String orderKey = RedisKeyPrefix.ORDER_INFO + request.getOrderNo();
                redisTemplate.delete(orderKey);

                log.info("订单支付成功: orderNo={}", request.getOrderNo());
                return Result.success(true);
            } else {
                // 订单状态更新失败，需要回滚Redis库存
                log.error("订单状态更新失败，回滚Redis库存: orderNo={}", request.getOrderNo());
                stockManagementService.rollbackRedisStock(request.getOrderNo());
                return Result.error("支付失败，订单状态更新失败");
            }
        } catch (Exception e) {
            log.error("支付订单失败", e);
            return Result.error("支付订单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Result<Boolean> cancelOrder(String orderNo) {
        // 检查参数
        if (orderNo == null) {
            return Result.error("订单号不能为空");
        }
        
        // 查询订单
        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (orderOpt.isEmpty()) {
            return Result.error("订单不存在");
        }
        
        Order order = orderOpt.get();
        
        // 检查订单状态
        if (!OrderStatus.WAITING_PAYMENT.getCode().equals(order.getStatus())) {
            return Result.error("订单状态不正确，无法取消");
        }
        
        try {
            // 取消回滚Redis：释放锁定库存
            boolean rollbackResult = stockManagementService.rollbackRedisStock(orderNo);
            if (!rollbackResult) {
                log.warn("回滚Redis库存失败，但继续取消订单: orderNo={}", orderNo);
            }

            // 更新订单状态为已取消
            int rows = orderRepository.updateOrderCancelled(
                    orderNo,
                    OrderStatus.CANCELLED.getCode(),
                    LocalDateTime.now()
            );

            if (rows > 0) {
                // 从Redis中删除订单信息
                String orderKey = RedisKeyPrefix.ORDER_INFO + orderNo;
                redisTemplate.delete(orderKey);

                log.info("订单取消成功: orderNo={}", orderNo);
                return Result.success(true);
            } else {
                return Result.error("取消订单失败，订单状态更新失败");
            }
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void handleTimeoutOrders() {
        // 查询已过期未支付的订单
        List<Order> timeoutOrders = orderRepository.findByStatusAndExpireTimeLessThan(
                OrderStatus.WAITING_PAYMENT.getCode(),
                LocalDateTime.now()
        );
        
        log.info("定时任务 - 处理超时未支付订单，共发现 {} 个超时订单", timeoutOrders.size());
        
        for (Order order : timeoutOrders) {
            try {
                // 更新订单状态为已取消
                int rows = orderRepository.updateOrderCancelled(
                        order.getOrderNo(),
                        OrderStatus.CANCELLED.getCode(),
                        LocalDateTime.now()
                );
                
                if (rows > 0) {
                    log.info("订单超时自动取消成功, orderNo: {}", order.getOrderNo());
                    
                    // 使用新的库存管理策略：直接回滚Redis库存
                    boolean rollbackResult = stockManagementService.rollbackRedisStock(order.getOrderNo());
                    if (!rollbackResult) {
                        log.warn("回滚Redis库存失败，但订单已取消: orderNo={}", order.getOrderNo());
                    }
                    
                    // 从Redis中删除订单信息
                    String orderKey = RedisKeyPrefix.ORDER_INFO + order.getOrderNo();
                    redisTemplate.delete(orderKey);
                } else {
                    log.warn("订单超时自动取消失败, orderNo: {}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("处理超时订单异常, orderNo: {}", order.getOrderNo(), e);
            }
        }
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> {
                    OrderDTO dto = new OrderDTO();
                    BeanUtils.copyProperties(order, dto);
                    return dto;
                })
                .toList();
    }

    @Override
    public List<OrderDTO> getOrdersByStatus(Integer status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(order -> {
                    OrderDTO dto = new OrderDTO();
                    BeanUtils.copyProperties(order, dto);
                    return dto;
                })
                .toList();
    }

    @Override
    public OrderDTO getOrderByOrderNo(String orderNo) {
        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (orderOpt.isPresent()) {
            OrderDTO dto = new OrderDTO();
            BeanUtils.copyProperties(orderOpt.get(), dto);
            return dto;
        }
        return null;
    }
} 