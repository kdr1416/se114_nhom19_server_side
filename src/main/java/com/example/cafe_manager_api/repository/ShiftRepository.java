package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<ShiftEntity, Integer> {

    @org.springframework.data.jpa.repository.Query("SELECT s FROM ShiftEntity s WHERE " +
        "(:shiftDate IS NULL OR s.shiftDate = :shiftDate) AND " +
        "(:status IS NULL OR s.status = :status)")
    List<ShiftEntity> filterShifts(
        @org.springframework.data.repository.query.Param("shiftDate") Long shiftDate,
        @org.springframework.data.repository.query.Param("status") String status
    );

    @Query("SELECT s FROM ShiftEntity s " +
           "WHERE s.shiftDate >= :startDate AND s.shiftDate <= :endDate " +
           "AND s.status = 'DRAFT' " +
           "ORDER BY s.shiftDate ASC")
    List<ShiftEntity> findDraftShiftsInRange(
        @Param("startDate") Long startDate,
        @Param("endDate") Long endDate
    );

    @Query("SELECT s FROM ShiftEntity s WHERE s.shiftDate >= :startDate AND s.shiftDate <= :endDate")
    List<ShiftEntity> findShiftsInRange(
        @Param("startDate") Long startDate,
        @Param("endDate") Long endDate
    );

    List<ShiftEntity> findByTemplateId(Integer templateId);
}
