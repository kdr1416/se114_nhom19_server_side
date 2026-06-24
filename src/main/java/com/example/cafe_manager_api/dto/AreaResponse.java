package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {
    private Integer areaId;
    private String areaName;
    private String prefix;
    private String description;
    private Integer tableCount;
}