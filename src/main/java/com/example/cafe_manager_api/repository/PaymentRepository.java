package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer> {
    Optional<PaymentEntity> findByOrderId(Integer orderId);

    List<PaymentEntity> findByPaidShiftIdAndStatus(Integer paidShiftId, String status);

    List<PaymentEntity> findByPaidShiftIdInAndStatus(List<Integer> paidShiftIds, String status);

    List<PaymentEntity> findByOrderIdIn(List<Integer> orderIds);

    @Query("SELECT SUM(p.finalAmount) FROM PaymentEntity p WHERE p.paidAt >= :start AND p.paidAt <= :end")
    Double getRevenueInRange(@Param("start") Long start, @Param("end") Long end);

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.paidAt >= :start AND p.paidAt <= :end")
    Long countPaymentsInRange(@Param("start") Long start, @Param("end") Long end);

    @Query("SELECT p FROM PaymentEntity p " +
           "WHERE p.status = 'PAID' " +
           "AND p.paidAt >= :startEpoch AND p.paidAt < :endEpoch")
    List<PaymentEntity> findPaidInRange(
            @Param("startEpoch") Long startEpoch,
            @Param("endEpoch") Long endEpoch);

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.cashierUserId = :userId AND p.paidShiftId IN :shiftIds")
    long countProcessedPaymentsByShiftIds(
            @Param("userId") Integer userId,
            @Param("shiftIds") List<Integer> shiftIds);

    @Query("SELECT COALESCE(SUM(p.finalAmount), 0.0) FROM PaymentEntity p WHERE p.cashierUserId = :userId AND p.paidShiftId IN :shiftIds")
    double sumRevenueByShiftIds(
            @Param("userId") Integer userId,
            @Param("shiftIds") List<Integer> shiftIds);

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.cashierUserId = :userId AND p.paidShiftId = :shiftId")
    long countProcessedPaymentsByShiftId(
            @Param("userId") Integer userId,
            @Param("shiftId") Integer shiftId);

    @Query("SELECT COALESCE(SUM(p.finalAmount), 0.0) FROM PaymentEntity p WHERE p.cashierUserId = :userId AND p.paidShiftId = :shiftId")
    double sumRevenueByShiftId(
            @Param("userId") Integer userId,
            @Param("shiftId") Integer shiftId);
}
