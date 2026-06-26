package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {
    private int year;
    private int month; // 1-12, 0 if querying full year
    private Double totalRevenue;
    private Integer orderCount;
    private Double avgOrderValue;
    private Double previousMonthRevenue;
    private Double growthPercent;
    private List<DailyRevenue> revenueByDay;
    private Map<String, Double> revenueByMethod;
    private Map<String, Integer> orderCountByMethod;
    private List<MonthlyRevenue> revenueByMonth;
    private Integer totalItemsSold;
    private List<ProductSoldResponse> itemsSold;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSoldResponse {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private Double revenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private int day;
        private String date; // "yyyy-MM-dd"
        private Double revenue;
        private Integer orderCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private int month; // 1-12
        private Double revenue;
        private Integer orderCount;
    }
}
