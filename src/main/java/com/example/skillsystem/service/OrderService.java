package com.example.skillsystem.service;

import com.example.skillsystem.dto.OrderDTO;
import com.example.skillsystem.vo.OrderRequest;
import com.example.skillsystem.vo.PaymentRequest;
import com.example.skillsystem.vo.Result;

import java.util.List;

public interface OrderService {
    
    /**
     * 创建订单
     * 
     * @param request 订单请求
     * @return 结果
     */
    Result<OrderDTO> createOrder(OrderRequest request);
    
    /**
     * 支付订单
     * 
     * @param request 支付请求
     * @return 结果
     */
    Result<Boolean> payOrder(PaymentRequest request);
    
    /**
     * 取消订单
     * 
     * @param orderNo 订单号
     * @return 结果
     */
    Result<Boolean> cancelOrder(String orderNo);
    
    /**
     * 处理超时未支付订单
     */
    void handleTimeoutOrders();
    
    /**
     * 获取所有订单
     * 
     * @return 订单列表
     */
    List<OrderDTO> getAllOrders();
    
    /**
     * 根据状态获取订单
     * 
     * @param status 订单状态
     * @return 订单列表
     */
    List<OrderDTO> getOrdersByStatus(Integer status);
    
    /**
     * 根据订单号获取订单
     * 
     * @param orderNo 订单号
     * @return 订单信息
     */
    OrderDTO getOrderByOrderNo(String orderNo);
} 