package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.AvailabilityWeekLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvailabilityWeekLockRepository extends JpaRepository<AvailabilityWeekLockEntity, Long> {
    boolean existsByWeekStart(Long weekStart);

    Optional<AvailabilityWeekLockEntity> findByWeekStart(Long weekStart);
}
