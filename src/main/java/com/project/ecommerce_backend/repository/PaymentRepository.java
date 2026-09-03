package com.project.ecommerce_backend.repository;


import com.project.ecommerce_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {




}
