package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplateEntity, Integer> {
    List<ShiftTemplateEntity> findByIsActiveTrue();

    @Query("SELECT t FROM ShiftTemplateEntity t " +
           "WHERE t.isActive = true " +
           "AND (t.effectiveFromDate IS NULL OR t.effectiveFromDate <= :currentTime) " +
           "AND (t.effectiveToDate IS NULL OR t.effectiveToDate >= :currentTime)")
    List<ShiftTemplateEntity> findActiveTemplates(@Param("currentTime") Long currentTime);
}
