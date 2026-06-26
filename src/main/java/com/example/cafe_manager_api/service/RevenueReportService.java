package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.RevenueReportResponse;
import com.example.cafe_manager_api.entity.PaymentEntity;
import com.example.cafe_manager_api.entity.OrderItemEntity;
import com.example.cafe_manager_api.repository.PaymentRepository;
import com.example.cafe_manager_api.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RevenueReportService {

    private final PaymentRepository paymentRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    public RevenueReportService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public RevenueReportResponse getMonthlyRevenue(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        long startEpoch = start.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long endEpoch = start.plusMonths(1).atStartOfDay(ZONE).toInstant().toEpochMilli();

        List<PaymentEntity> payments = paymentRepository.findPaidInRange(startEpoch, endEpoch);

        double totalRevenue = payments.stream()
                .mapToDouble(PaymentEntity::getFinalAmount)
                .sum();
        int orderCount = payments.size();
        double avgOrderValue = orderCount > 0 ? totalRevenue / orderCount : 0.0;

        // Initialize payment method maps
        Map<String, Double> revenueByMethod = new LinkedHashMap<>();
        revenueByMethod.put("CASH", 0.0);
        revenueByMethod.put("TRANSFER", 0.0);
        revenueByMethod.put("MOMO", 0.0);

        Map<String, Integer> orderCountByMethod = new LinkedHashMap<>();
        orderCountByMethod.put("CASH", 0);
        orderCountByMethod.put("TRANSFER", 0);
        orderCountByMethod.put("MOMO", 0);

        for (PaymentEntity p : payments) {
            if (p.getPaymentMethod() != null) {
                String method = p.getPaymentMethod().toUpperCase();
                revenueByMethod.put(method, revenueByMethod.getOrDefault(method, 0.0) + p.getFinalAmount());
                orderCountByMethod.put(method, orderCountByMethod.getOrDefault(method, 0) + 1);
            }
        }

        // Group payments by day of month
        Map<Integer, List<PaymentEntity>> paymentsByDay = payments.stream()
                .collect(Collectors.groupingBy(p ->
                        Instant.ofEpochMilli(p.getPaidAt()).atZone(ZONE).toLocalDate().getDayOfMonth()
                ));

        List<RevenueReportResponse.DailyRevenue> revenueByDay = new ArrayList<>();
        int daysInMonth = start.lengthOfMonth();
        for (int i = 1; i <= daysInMonth; i++) {
            List<PaymentEntity> dayPayments = paymentsByDay.getOrDefault(i, Collections.emptyList());
            double dayRevenue = dayPayments.stream().mapToDouble(PaymentEntity::getFinalAmount).sum();
            int dayOrderCount = dayPayments.size();
            String dateStr = String.format("%04d-%02d-%02d", year, month, i);
            revenueByDay.add(new RevenueReportResponse.DailyRevenue(i, dateStr, dayRevenue, dayOrderCount));
        }

        // Calculate previous month revenue
        int prevYear = (month == 1) ? year - 1 : year;
        int prevMonth = (month == 1) ? 12 : month - 1;
        LocalDate prevStart = LocalDate.of(prevYear, prevMonth, 1);
        long prevStartEpoch = prevStart.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long prevEndEpoch = prevStart.plusMonths(1).atStartOfDay(ZONE).toInstant().toEpochMilli();

        List<PaymentEntity> prevPayments = paymentRepository.findPaidInRange(prevStartEpoch, prevEndEpoch);
        double previousMonthRevenue = prevPayments.stream()
                .mapToDouble(PaymentEntity::getFinalAmount)
                .sum();

        Double growthPercent = null;
        if (previousMonthRevenue > 0.0) {
            growthPercent = ((totalRevenue - previousMonthRevenue) / previousMonthRevenue) * 100.0;
        }

        // Aggregate items sold
        List<Integer> orderIds = payments.stream()
                .map(PaymentEntity::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<OrderItemEntity> orderItems = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            orderItems = orderItemRepository.findByOrderIdIn(orderIds);
        }

        Map<Integer, RevenueReportResponse.ProductSoldResponse> productSummaryMap = new HashMap<>();
        int totalItemsSold = 0;
        for (OrderItemEntity item : orderItems) {
            int pId = item.getProductId();
            String pName = item.getProductNameSnapshot();
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            double sub = item.getSubtotal() != null ? item.getSubtotal() : 0.0;

            totalItemsSold += qty;

            RevenueReportResponse.ProductSoldResponse prodSummary = productSummaryMap.get(pId);
            if (prodSummary == null) {
                prodSummary = new RevenueReportResponse.ProductSoldResponse(pId, pName, 0, 0.0);
            }
            prodSummary.setQuantity(prodSummary.getQuantity() + qty);
            prodSummary.setRevenue(prodSummary.getRevenue() + sub);
            productSummaryMap.put(pId, prodSummary);
        }
        List<RevenueReportResponse.ProductSoldResponse> itemsSold = new ArrayList<>(productSummaryMap.values());
        itemsSold.sort((a, b) -> b.getQuantity().compareTo(a.getQuantity()));

        return new RevenueReportResponse(
                year,
                month,
                totalRevenue,
                orderCount,
                avgOrderValue,
                previousMonthRevenue,
                growthPercent,
                revenueByDay,
                revenueByMethod,
                orderCountByMethod,
                new ArrayList<>(),
                totalItemsSold,
                itemsSold
        );
    }

    public RevenueReportResponse getYearlySummary(int year) {
        double totalRevenue = 0.0;
        int orderCount = 0;

        Map<String, Double> revenueByMethod = new LinkedHashMap<>();
        revenueByMethod.put("CASH", 0.0);
        revenueByMethod.put("TRANSFER", 0.0);
        revenueByMethod.put("MOMO", 0.0);

        Map<String, Integer> orderCountByMethod = new LinkedHashMap<>();
        orderCountByMethod.put("CASH", 0);
        orderCountByMethod.put("TRANSFER", 0);
        orderCountByMethod.put("MOMO", 0);

        List<RevenueReportResponse.MonthlyRevenue> revenueByMonth = new ArrayList<>();
        int yearlyTotalItemsSold = 0;
        Map<Integer, RevenueReportResponse.ProductSoldResponse> yearlyProductMap = new HashMap<>();

        for (int m = 1; m <= 12; m++) {
            RevenueReportResponse monthly = getMonthlyRevenue(year, m);
            totalRevenue += monthly.getTotalRevenue();
            orderCount += monthly.getOrderCount();

            // Accumulate methods
            for (Map.Entry<String, Double> entry : monthly.getRevenueByMethod().entrySet()) {
                String method = entry.getKey();
                revenueByMethod.put(method, revenueByMethod.getOrDefault(method, 0.0) + entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : monthly.getOrderCountByMethod().entrySet()) {
                String method = entry.getKey();
                orderCountByMethod.put(method, orderCountByMethod.getOrDefault(method, 0) + entry.getValue());
            }

            // Accumulate products
            yearlyTotalItemsSold += monthly.getTotalItemsSold();
            if (monthly.getItemsSold() != null) {
                for (RevenueReportResponse.ProductSoldResponse p : monthly.getItemsSold()) {
                    RevenueReportResponse.ProductSoldResponse yProd = yearlyProductMap.get(p.getProductId());
                    if (yProd == null) {
                        yProd = new RevenueReportResponse.ProductSoldResponse(p.getProductId(), p.getProductName(), 0, 0.0);
                    }
                    yProd.setQuantity(yProd.getQuantity() + p.getQuantity());
                    yProd.setRevenue(yProd.getRevenue() + p.getRevenue());
                    yearlyProductMap.put(p.getProductId(), yProd);
                }
            }

            revenueByMonth.add(new RevenueReportResponse.MonthlyRevenue(m, monthly.getTotalRevenue(), monthly.getOrderCount()));
        }

        double avgOrderValue = orderCount > 0 ? totalRevenue / orderCount : 0.0;
        
        List<RevenueReportResponse.ProductSoldResponse> itemsSold = new ArrayList<>(yearlyProductMap.values());
        itemsSold.sort((a, b) -> b.getQuantity().compareTo(a.getQuantity()));

        return new RevenueReportResponse(
                year,
                0, // 0 indicates full year query
                totalRevenue,
                orderCount,
                avgOrderValue,
                null,
                null,
                new ArrayList<>(),
                revenueByMethod,
                orderCountByMethod,
                revenueByMonth,
                yearlyTotalItemsSold,
                itemsSold
        );
    }
}
