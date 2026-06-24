package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftReportResponse {
    private Integer shiftId;
    private String shiftName;
    private Long shiftDate;
    private String status;
    private Double openingCash;
    private Double closingCash;
    private Integer totalOrders;
    private Double totalRevenue;
    private List<UserProfileResponse> assignedStaff;
}
