package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer> {
    List<AttendanceEntity> findByUserId(Integer userId);
    List<AttendanceEntity> findByShiftId(Integer shiftId);
    AttendanceEntity findByShiftIdAndUserId(Integer shiftId, Integer userId);

    @Query("SELECT a FROM AttendanceEntity a WHERE a.shiftId IN :shiftIds")
    List<AttendanceEntity> findByShiftIdIn(@Param("shiftIds") List<Integer> shiftIds);
}
