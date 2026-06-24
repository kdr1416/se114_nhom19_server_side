package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    List<OrderEntity> findByTableIdAndStatus(Integer tableId, String status);
    List<OrderEntity> findByStatus(String status);
    List<OrderEntity> findByCreatedShiftId(Integer createdShiftId);
}
