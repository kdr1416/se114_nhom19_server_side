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

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.finalAmount), 0) FROM PaymentEntity p " +
           "WHERE p.paidShiftId = :shiftId AND p.paymentMethod = :paymentMethod")
    double sumCashPaymentsByShift(
            @org.springframework.data.repository.query.Param("shiftId") Integer shiftId,
            @org.springframework.data.repository.query.Param("paymentMethod") String paymentMethod);
}
