package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shifts")
public class ShiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "shift_name")
    private String shiftName;

    @Column(name = "shift_date")
    private Long shiftDate;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "status")
    private String status;

    @Column(name = "opened_by")
    private Integer openedBy;

    @Column(name = "opened_at")
    private Long openedAt;

    @Column(name = "closed_by")
    private Integer closedBy;

    @Column(name = "closed_at")
    private Long closedAt;

    @Column(name = "opening_cash")
    private Double openingCash;

    @Column(name = "closing_cash")
    private Double closingCash;

    @Column(name = "created_at")
    private Long createdAt;
}
