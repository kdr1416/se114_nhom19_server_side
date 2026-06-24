package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class UpdateAreaRequest {
    private String areaName;
    private String prefix;
    private String description;
}