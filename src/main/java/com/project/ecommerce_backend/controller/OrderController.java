package com.project.ecommerce_backend.controller;


import com.project.ecommerce_backend.dto.OrderItemResponse;
import com.project.ecommerce_backend.dto.OrderResponse;
import com.project.ecommerce_backend.security.UserPrincipal;
import com.project.ecommerce_backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(Authentication authentication) {
        Long userId = getUserId(authentication);
        OrderResponse order = orderService.createOrder(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<OrderResponse> orders = orderService.getOrdersForUser(userId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderResponse> payOrder(@PathVariable Long orderId, Authentication authentication) {
        Long userId = getUserId(authentication);
        OrderResponse response = orderService.processPayment(userId, orderId);
        return ResponseEntity.ok(response);
    }

    private Long getUserId(Authentication authentication) {
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("Principal is not UserPrincipal");
    }
}
