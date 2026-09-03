package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.PaymentResult;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult charge(Long orderId, BigDecimal amount);
}
