package com.example.skillsystem.controller;

import com.example.skillsystem.dto.ProductDTO;
import com.example.skillsystem.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ProductService productService;

    @GetMapping("/quantity")
    public String quantityTest() {
        return "test/quantity-test";
    }

    @GetMapping("/product/{id}")
    @ResponseBody
    public ProductDTO testProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/product-page/{id}")
    public String testProductPage(@PathVariable Long id, Model model) {
        ProductDTO product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("debug", true);
        return "test/product-debug";
    }
}
