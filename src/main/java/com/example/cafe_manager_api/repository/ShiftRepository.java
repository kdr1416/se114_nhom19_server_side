package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<ShiftEntity, Integer> {

    @org.springframework.data.jpa.repository.Query("SELECT s FROM ShiftEntity s WHERE " +
        "(:shiftDate IS NULL OR s.shiftDate = :shiftDate) AND " +
        "(:status IS NULL OR UPPER(s.status) = UPPER(:status))")
    List<ShiftEntity> filterShifts(
        @org.springframework.data.repository.query.Param("shiftDate") Long shiftDate,
        @org.springframework.data.repository.query.Param("status") String status
    );
}
