package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.entity.PromotionEntity;
import com.example.cafe_manager_api.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionEntity>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Long> getTotalCount() {
        return ResponseEntity.ok(promotionService.getTotalCount());
    }

    @GetMapping("/active-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Long> getActiveCount() {
        return ResponseEntity.ok(promotionService.getActiveCount());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PromotionEntity> getPromotionByCode(@PathVariable String code) {
        return ResponseEntity.ok(promotionService.getPromotionByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionEntity> createPromotion(@Valid @RequestBody PromotionEntity promotion) {
        return ResponseEntity.ok(promotionService.createPromotion(promotion));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionEntity> updatePromotion(@PathVariable Integer id, @Valid @RequestBody PromotionEntity promotion) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, promotion));
    }

    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> toggleActive(@PathVariable Integer id, @RequestParam boolean active) {
        promotionService.toggleActive(id, active);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deletePromotion(@PathVariable Integer id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok().build();
    }
}
