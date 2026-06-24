package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "table_id")
    private Integer tableId;

    @Column(name = "order_code")
    private String orderCode;

    @Column(name = "status")
    private String status;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "paid_at")
    private Long paidAt;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column(name = "created_shift_id")
    private Integer createdShiftId;
}
