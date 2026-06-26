package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyShiftReportResponse {
    private String date; // yyyy-MM-dd
    private Double totalRevenue;
    private Integer totalOrders;
    private Integer paymentCount;
    private Double totalOpeningCash;
    private Double totalExpectedCash;
    private List<ShiftSummary> shifts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSummary {
        private Integer shiftId;
        private String shiftName;
        private String startTime;
        private String endTime;
        private String status;
        private Double revenue;
        private Integer orderCount;
        private Double openingCash;
        private Double expectedCash;
    }
}
