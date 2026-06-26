package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeekLockResponse {
    private boolean locked;
    private Long lockedAt;
    private Integer lockedBy;
}
