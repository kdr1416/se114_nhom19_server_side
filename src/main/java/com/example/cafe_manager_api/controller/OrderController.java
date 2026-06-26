package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.*;
import com.example.cafe_manager_api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request, Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        OrderResponse response = orderService.createOrder(request, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders(@RequestParam(required = false) String status) {
        List<OrderResponse> responses = orderService.getAllOrders(status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrderById(@PathVariable Integer id) {
        OrderDetailResponse response = orderService.getOrderDetail(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDetailResponse> addItem(
            @PathVariable Integer id,
            @Valid @RequestBody OrderItemRequest request) {
        OrderDetailResponse response = orderService.addItem(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderDetailResponse> updateItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestBody(required = false) Map<String, Integer> body,
            @RequestParam(required = false) Integer quantity) {
        Integer q = null;
        if (body != null && body.containsKey("quantity")) {
            q = body.get("quantity");
        } else if (quantity != null) {
            q = quantity;
        }
        if (q == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu cung cấp số lượng (quantity) trong body hoặc query param.");
        }
        OrderDetailResponse response = orderService.updateItem(id, itemId, q);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderDetailResponse> removeItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId) {
        OrderDetailResponse response = orderService.removeItem(id, itemId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable Integer id) {
        OrderResponse response = orderService.confirmOrder(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Integer id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderDetailResponse>> getPaidOrdersHistory(
            @RequestParam Long from,
            @RequestParam Long to) {
        List<OrderDetailResponse> responses = orderService.getPaidOrdersHistory(from, to);
        return ResponseEntity.ok(responses);
    }
}
