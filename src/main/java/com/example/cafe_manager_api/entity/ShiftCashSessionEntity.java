package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift_cash_sessions")
public class ShiftCashSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "opening_cash")
    private Double openingCash;

    @Column(name = "closing_cash")
    private Double closingCash;

    @Column(name = "expected_cash")
    private Double expectedCash;

    @Column(name = "actual_cash")
    private Double actualCash;

    @Column(name = "cash_difference")
    private Double cashDifference;

    @Column(name = "opened_by")
    private Integer openedBy;

    @Column(name = "opened_at")
    private Long openedAt;

    @Column(name = "closed_by")
    private Integer closedBy;

    @Column(name = "closed_at")
    private Long closedAt;

    @Column(name = "status")
    private String status;
}
