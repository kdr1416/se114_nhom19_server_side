package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class CheckInRequest {
    private Integer shiftId;
    private Double latitude;
    private Double longitude;
}
