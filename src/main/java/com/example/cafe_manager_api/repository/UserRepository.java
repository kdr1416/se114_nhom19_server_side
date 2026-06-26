package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);
    java.util.List<UserEntity> findByRole(String role);

    @Query("SELECT u FROM UserEntity u WHERE u.userId IN :ids")
    List<UserEntity> findAllByUserIdIn(@Param("ids") List<Integer> ids);

    @Query(value = """
        SELECT u.* FROM users u
        JOIN employee_weekly_availability a ON u.user_id = a.user_id
        WHERE a.template_id = :templateId
          AND a.day_of_week = :dayOfWeek
          AND a.is_available = true
          AND (a.effective_from_date IS NULL OR a.effective_from_date <= :shiftDate)
          AND (a.effective_to_date IS NULL OR a.effective_to_date >= :shiftDate)
          AND (a.week_start IS NULL OR a.week_start = :weekStart)
          AND a.status = 'PUBLISHED'
          AND u.is_active = true
        """, nativeQuery = true)
    List<UserEntity> findAvailableStaff(
        @Param("templateId") Integer templateId,
        @Param("dayOfWeek") Integer dayOfWeek,
        @Param("shiftDate") Long shiftDate,
        @Param("weekStart") Long weekStart);
}
