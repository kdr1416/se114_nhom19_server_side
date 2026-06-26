package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;

@Entity
@Table(name = "leave_requests",
    indexes = {
        @Index(name = "idx_leave_user_id", columnList = "user_id"),
        @Index(name = "idx_leave_status", columnList = "status"),
        @Index(name = "idx_leave_time_range", columnList = "start_at, end_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_request_id")
    private Long leaveRequestId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "start_at", nullable = false)
    private Long startAt;  // epoch millis

    @Column(name = "end_at", nullable = false)
    private Long endAt;    // epoch millis

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    @Column(name = "reviewed_by_user_id")
    private Integer reviewedByUserId;

    @Column(name = "reviewed_at")
    private Long reviewedAt;  // epoch millis

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;
}
