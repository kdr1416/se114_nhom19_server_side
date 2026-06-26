package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.SchedulingRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulingRunRepository extends JpaRepository<SchedulingRunEntity, Long> {

    List<SchedulingRunEntity> findByCreatedByOrderByCreatedAtDesc(Integer createdBy);

    List<SchedulingRunEntity> findByStatus(String status);
}
