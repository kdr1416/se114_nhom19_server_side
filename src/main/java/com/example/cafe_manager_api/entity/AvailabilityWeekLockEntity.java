package com.example.cafe_manager_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "availability_week_locks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityWeekLockEntity {

    @Id
    @Column(name = "week_start")
    private Long weekStart;  // epoch millis Monday 00:00 Asia/Ho_Chi_Minh, not auto-generated

    @Column(name = "locked_at", nullable = false)
    private Long lockedAt;

    @Column(name = "locked_by", nullable = false)
    private Integer lockedBy;  // userId of manager who triggered scheduling
}
