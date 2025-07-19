package com.example.skillsystem.controller;

import com.example.skillsystem.dto.OrderDTO;
import com.example.skillsystem.service.OrderService;
import com.example.skillsystem.vo.OrderRequest;
import com.example.skillsystem.vo.PaymentRequest;
import com.example.skillsystem.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public Result<OrderDTO> createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
    
    @PostMapping("/payment")
    public Result<Boolean> payOrder(@RequestBody PaymentRequest request) {
        return orderService.payOrder(request);
    }
    
    @PostMapping("/cancel/{orderNo}")
    public Result<Boolean> cancelOrder(@PathVariable String orderNo) {
        return orderService.cancelOrder(orderNo);
    }
} 