package com.project.ecommerce_backend.service;

import com.project.ecommerce_backend.dto.PaymentResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway{

    @Override
    public PaymentResult charge(Long orderId, BigDecimal amount) {
        //simulate a successful payment 90% of the time.
        boolean success = Math.random() < 0.9;
        String transactionId = success ? "TXN-" + UUID.randomUUID() : null;
        return new PaymentResult(success, transactionId);
    }


}
