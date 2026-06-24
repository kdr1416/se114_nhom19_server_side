package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.entity.PromotionEntity;
import com.example.cafe_manager_api.repository.PromotionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Transactional(readOnly = true)
    public List<PromotionEntity> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long getTotalCount() {
        return promotionRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveCount() {
        return promotionRepository.countByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public PromotionEntity getPromotionByCode(String code) {
        return promotionRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã khuyến mãi: " + code));
    }

    @Transactional
    public PromotionEntity createPromotion(PromotionEntity promotion) {
        Optional<PromotionEntity> existing = promotionRepository.findByCode(promotion.getCode().trim().toUpperCase());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại: " + promotion.getCode());
        }
        promotion.setCode(promotion.getCode().trim().toUpperCase());
        if (promotion.getIsActive() == null) {
            promotion.setIsActive(true);
        }
        if (promotion.getCreatedAt() == null || promotion.getCreatedAt() <= 0) {
            promotion.setCreatedAt(System.currentTimeMillis());
        }
        return promotionRepository.save(promotion);
    }

    @Transactional
    public PromotionEntity updatePromotion(Integer id, PromotionEntity details) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã giảm giá với ID: " + id));
        promotion.setCode(details.getCode().trim().toUpperCase());
        promotion.setType(details.getType());
        promotion.setValue(details.getValue());
        if (details.getIsActive() != null) {
            promotion.setIsActive(details.getIsActive());
        }
        promotion.setExpiresAt(details.getExpiresAt());
        return promotionRepository.save(promotion);
    }

    @Transactional
    public void toggleActive(Integer id, boolean active) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã giảm giá với ID: " + id));
        promotion.setIsActive(active);
        promotionRepository.save(promotion);
    }

    @Transactional
    public void deletePromotion(Integer id) {
        if (!promotionRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy mã giảm giá với ID: " + id);
        }
        promotionRepository.deleteById(id);
    }
}
