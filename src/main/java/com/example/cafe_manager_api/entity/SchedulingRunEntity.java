package com.example.cafe_manager_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "scheduling_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long runId;

    @Column(name = "start_date", nullable = false)
    private Long startDate;  // epoch millis — range start

    @Column(name = "end_date", nullable = false)
    private Long endDate;    // epoch millis — range end

    @Column(name = "status", nullable = false)
    private String status;   // "PREVIEW" or "APPLIED"

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;  // userId of manager who triggered

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "applied_at")
    private Long appliedAt;  // nullable

    @Column(name = "suggestions_json", columnDefinition = "TEXT")
    private String suggestionsJson;
}
