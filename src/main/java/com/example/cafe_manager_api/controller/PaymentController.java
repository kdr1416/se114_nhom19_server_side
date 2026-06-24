package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.PaymentRequest;
import com.example.cafe_manager_api.dto.PaymentResponse;
import com.example.cafe_manager_api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        PaymentResponse response = paymentService.processPayment(request, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<com.example.cafe_manager_api.entity.PaymentEntity> getPaymentByOrderId(@PathVariable Integer orderId) {
        com.example.cafe_manager_api.entity.PaymentEntity response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}
