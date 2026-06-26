package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.EmployeeWeeklyAvailabilityEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAvailabilityRepository
    extends JpaRepository<EmployeeWeeklyAvailabilityEntity, Integer> {

    List<EmployeeWeeklyAvailabilityEntity> findByUserId(Integer userId);

    List<EmployeeWeeklyAvailabilityEntity> findByTemplateId(Integer templateId);

    Optional<EmployeeWeeklyAvailabilityEntity> findByUserIdAndTemplateIdAndDayOfWeek(
        Integer userId, Integer templateId, Integer dayOfWeek);

    Optional<EmployeeWeeklyAvailabilityEntity> findByUserIdAndTemplateIdAndDayOfWeekAndWeekStart(
        Integer userId, Integer templateId, Integer dayOfWeek, Long weekStart);

    // Find all available staff (active users) for a template+dayOfWeek
    // Used by scheduling engine in Phase 4
    @Query("SELECT a FROM EmployeeWeeklyAvailabilityEntity a " +
           "WHERE a.templateId = :templateId " +
           "AND a.dayOfWeek = :dayOfWeek " +
           "AND a.isAvailable = true " +
           "AND (a.effectiveFromDate IS NULL OR a.effectiveFromDate <= :shiftDate) " +
           "AND (a.effectiveToDate IS NULL OR a.effectiveToDate >= :shiftDate) " +
           "AND (a.weekStart IS NULL OR a.weekStart = :weekStart)")
    List<EmployeeWeeklyAvailabilityEntity> findAvailableForShift(
        @Param("templateId") Integer templateId,
        @Param("dayOfWeek") Integer dayOfWeek,
        @Param("shiftDate") Long shiftDate,
        @Param("weekStart") Long weekStart);

    @Query("SELECT a FROM EmployeeWeeklyAvailabilityEntity a " +
           "WHERE a.templateId IN :templateIds " +
           "AND a.isAvailable = true " +
           "AND (a.weekStart IS NULL OR a.weekStart <= :endDate) " +
           "AND (a.publishedUntil IS NULL OR a.publishedUntil >= :startDate)")
    List<EmployeeWeeklyAvailabilityEntity> findAvailableForTemplates(
        @Param("templateIds") List<Integer> templateIds,
        @Param("startDate") Long startDate,
        @Param("endDate") Long endDate);
}
