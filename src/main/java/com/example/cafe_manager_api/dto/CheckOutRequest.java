package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class CheckOutRequest {
    private Integer shiftId;
    private String notes;
}
