package com.example.skillsystem.controller;

import com.example.skillsystem.dto.OrderDTO;
import com.example.skillsystem.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderViewController {

    private final OrderService orderService;

    @GetMapping("/list")
    public String orderList(@RequestParam(required = false) Integer status, Model model) {
        List<OrderDTO> orders;
        if (status != null) {
            orders = orderService.getOrdersByStatus(status);
        } else {
            orders = orderService.getAllOrders();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        return "order/list";
    }
    
    @GetMapping("/payment")
    public String orderPayment(@RequestParam String orderNo, Model model) {
        OrderDTO order = orderService.getOrderByOrderNo(orderNo);
        model.addAttribute("order", order);
        return "order/payment";
    }

    @GetMapping("/payment/{orderNo}")
    public String orderPaymentByPath(@PathVariable String orderNo, Model model) {
        OrderDTO order = orderService.getOrderByOrderNo(orderNo);
        model.addAttribute("order", order);
        return "order/payment";
    }
} 