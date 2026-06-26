package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.LeaveRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {
    List<LeaveRequestEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<LeaveRequestEntity> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT l FROM LeaveRequestEntity l " +
           "WHERE l.userId = :userId " +
           "AND l.status IN ('PENDING', 'APPROVED') " +
           "AND l.startAt < :endAt AND l.endAt > :startAt")
    List<LeaveRequestEntity> findOverlappingRequests(@Param("userId") Integer userId,
                                                      @Param("startAt") Long startAt,
                                                      @Param("endAt") Long endAt);

    @Query("SELECT COUNT(l) > 0 FROM LeaveRequestEntity l " +
           "WHERE l.userId = :userId " +
           "AND l.status = 'APPROVED' " +
           "AND l.startAt < :shiftEnd AND l.endAt > :shiftStart")
    boolean hasApprovedLeaveOverlap(@Param("userId") Integer userId,
                                    @Param("shiftStart") Long shiftStart,
                                    @Param("shiftEnd") Long shiftEnd);

    @Query("SELECT l FROM LeaveRequestEntity l " +
           "WHERE l.status = 'APPROVED' " +
           "AND l.startAt < :endAt AND l.endAt > :startAt")
    List<LeaveRequestEntity> findApprovedLeavesInRange(@Param("startAt") Long startAt,
                                                       @Param("endAt") Long endDate);
}
