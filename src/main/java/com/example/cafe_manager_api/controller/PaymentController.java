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

import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import java.util.List;

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
    public ResponseEntity<com.example.cafe_manager_api.dto.PaymentResponse> getPaymentByOrderId(@PathVariable Integer orderId) {
        com.example.cafe_manager_api.dto.PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<com.example.cafe_manager_api.dto.PaymentResponse>> getPaymentsInRange(
            @RequestParam("startDate") Long startDate,
            @RequestParam("endDate") Long endDate) {
        List<com.example.cafe_manager_api.dto.PaymentResponse> payments = paymentService.getPaymentsInRange(startDate, endDate);
        return ResponseEntity.ok(payments);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/revenue")
    public ResponseEntity<Double> getRevenueInRange(
            @RequestParam("startDate") Long startDate,
            @RequestParam("endDate") Long endDate) {
        Double revenue = paymentService.getRevenueInRange(startDate, endDate);
        return ResponseEntity.ok(revenue);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/count")
    public ResponseEntity<Long> countPaymentsInRange(
            @RequestParam("startDate") Long startDate,
            @RequestParam("endDate") Long endDate) {
        Long count = paymentService.countPaymentsInRange(startDate, endDate);
        return ResponseEntity.ok(count);
    }
}
