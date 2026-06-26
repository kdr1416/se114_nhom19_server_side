package com.example.cafe_manager_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "employee_weekly_availability",
    indexes = {
        @Index(name = "idx_availability_user_id", columnList = "user_id"),
        @Index(name = "idx_availability_template_id", columnList = "template_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uc_user_template_day",
            columnNames = {"user_id", "template_id", "day_of_week"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWeeklyAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")
    private Integer availabilityId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    @Column(name = "day_of_week", nullable = false)
    // 1=Monday, 2=Tuesday, ... 7=Sunday (ISO standard)
    private Integer dayOfWeek;

    @Column(name = "effective_from_date")
    private Long effectiveFromDate;  // nullable - epoch millis

    @Column(name = "effective_to_date")
    private Long effectiveToDate;    // nullable - epoch millis, null = indefinite

    @Column(name = "status", nullable = false)
    private String status = "PUBLISHED";
    // Values: "DRAFT" | "PUBLISHED"

    @Column(name = "week_start")
    private Long weekStart;
    // epoch millis of Monday 00:00:00.000 Asia/Ho_Chi_Minh of the week this record applies to
    // null = applies to all weeks (legacy records)

    @Column(name = "published_until")
    private Long publishedUntil;
    // epoch millis of last applicable Monday. null = only this week (scope = THIS_WEEK)

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;  // nullable
}
