package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShiftCashSessionRepository extends JpaRepository<ShiftCashSessionEntity, Integer> {
    Optional<ShiftCashSessionEntity> findByShiftId(Integer shiftId);
}
