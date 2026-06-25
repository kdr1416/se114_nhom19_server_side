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

    // Expanded report fields
    private Double cashRevenue;
    private Double transferRevenue;
    private Double momoRevenue;
    private Integer unpaidOrders;
    private Integer paymentCount;
    private Double expectedCash;
    private Double cashDifference;
    private Integer staffPresentCount;
    private List<PaymentMethodStatsResponse> paymentMethodStats;
    private List<ProductSoldSummary> topProducts;
    private List<StaffAttendanceSummary> attendanceList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodStatsResponse {
        private String paymentMethod;
        private Integer orderCount;
        private Double totalRevenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSoldSummary {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private Double subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffAttendanceSummary {
        private Integer userId;
        private String username;
        private String fullName;
        private Long checkInAt;
        private Long checkOutAt;
        private String status;
        private String notes;
    }
}
