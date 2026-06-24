package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer> {
    Optional<PaymentEntity> findByOrderId(Integer orderId);

    @Query("SELECT SUM(p.finalAmount) FROM PaymentEntity p WHERE p.paidAt >= :start AND p.paidAt <= :end")
    Double getRevenueInRange(@Param("start") Long start, @Param("end") Long end);

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.paidAt >= :start AND p.paidAt <= :end")
    Long countPaymentsInRange(@Param("start") Long start, @Param("end") Long end);
}
