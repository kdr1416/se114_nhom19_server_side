package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "final_amount")
    private Double finalAmount;

    @Column(name = "paid_at")
    private Long paidAt;

    @Column(name = "status")
    private String status;

    @Column(name = "cashier_user_id")
    private Integer cashierUserId;

    @Column(name = "paid_shift_id")
    private Integer paidShiftId;
}
