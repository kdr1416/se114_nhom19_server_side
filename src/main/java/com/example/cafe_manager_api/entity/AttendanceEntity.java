package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"shift_id", "user_id"}))
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Integer attendanceId;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "check_in_at")
    private Long checkInAt;

    @Column(name = "check_out_at")
    private Long checkOutAt;

    @Column(name = "status")
    private String status;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes;

    @Column(name = "notes")
    private String notes;
}
