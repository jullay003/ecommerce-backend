package com.project.ecommerce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private int quantity;
    private BigDecimal price;
}