package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignmentEntity, Integer> {
    List<ShiftAssignmentEntity> findByShiftId(Integer shiftId);
    List<ShiftAssignmentEntity> findByUserId(Integer userId);
    java.util.Optional<ShiftAssignmentEntity> findByShiftIdAndUserId(Integer shiftId, Integer userId);

    @Query("SELECT COUNT(a) > 0 FROM ShiftAssignmentEntity a " +
           "JOIN ShiftEntity s ON s.shiftId = a.shiftId " +
           "WHERE a.userId = :userId " +
           "AND s.shiftDate = :shiftDate " +
           "AND s.status NOT IN ('CANCELLED', 'CLOSED') " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    boolean existsOverlappingAssignment(@Param("userId") Integer userId,
                                        @Param("shiftDate") Long shiftDate,
                                        @Param("startTime") String startTime,
                                        @Param("endTime") String endTime);

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT COUNT(*) FROM shift_assignments sa " +
        "JOIN shifts s ON sa.shift_id = s.shift_id " +
        "WHERE sa.user_id = :userId " +
        "AND s.shift_date = :shiftDate " +
        "AND s.status NOT IN ('CANCELLED') " +
        "AND (s.start_time < :endTime AND s.end_time > :startTime)",
        nativeQuery = true)
    long countOverlappingShifts(
        @org.springframework.data.repository.query.Param("userId") Integer userId,
        @org.springframework.data.repository.query.Param("shiftDate") Long shiftDate,
        @org.springframework.data.repository.query.Param("startTime") String startTime,
        @org.springframework.data.repository.query.Param("endTime") String endTime
    );

    @Query("SELECT a FROM ShiftAssignmentEntity a " +
           "JOIN ShiftEntity s ON s.shiftId = a.shiftId " +
           "WHERE s.shiftDate >= :startDate AND s.shiftDate <= :endDate " +
           "AND s.status NOT IN ('CANCELLED')")
    List<ShiftAssignmentEntity> findAssignmentsInDateRange(@Param("startDate") Long startDate,
                                                           @Param("endDate") Long endDate);
}
