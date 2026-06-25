package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.*;
import com.example.cafe_manager_api.entity.*;
import com.example.cafe_manager_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShiftReportService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Transactional(readOnly = true)
    public ShiftReportResponse getShiftReport(Integer shiftId) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        // 1. Get payments for this shift
        List<PaymentEntity> payments = paymentRepository.findByPaidShiftIdAndStatus(shiftId, "PAID");
        int paymentCount = payments.size();

        double totalRevenue = 0.0;
        double cashRevenue = 0.0;
        double transferRevenue = 0.0;
        double momoRevenue = 0.0;

        Map<String, ShiftReportResponse.PaymentMethodStatsResponse> methodStatsMap = new HashMap<>();

        for (PaymentEntity p : payments) {
            double amount = p.getFinalAmount() != null ? p.getFinalAmount() : 0.0;
            totalRevenue += amount;

            String method = p.getPaymentMethod() != null ? p.getPaymentMethod().trim().toUpperCase() : "UNKNOWN";
            if ("CASH".equals(method)) {
                cashRevenue += amount;
            } else if ("TRANSFER".equals(method)) {
                transferRevenue += amount;
            } else if ("MOMO".equals(method)) {
                momoRevenue += amount;
            }

            ShiftReportResponse.PaymentMethodStatsResponse stats = methodStatsMap.get(method);
            if (stats == null) {
                stats = new ShiftReportResponse.PaymentMethodStatsResponse(method, 0, 0.0);
            }
            stats.setOrderCount(stats.getOrderCount() + 1);
            stats.setTotalRevenue(stats.getTotalRevenue() + amount);
            methodStatsMap.put(method, stats);
        }

        List<ShiftReportResponse.PaymentMethodStatsResponse> paymentMethodStats = new ArrayList<>(methodStatsMap.values());

        // 2. Expected cash & Difference
        double openingCash = shift.getOpeningCash() != null ? shift.getOpeningCash() : 0.0;
        double expectedCash = openingCash + cashRevenue;
        double closingCash = shift.getClosingCash() != null ? shift.getClosingCash() : 0.0;
        double cashDifference = 0.0;
        if ("CLOSED".equalsIgnoreCase(shift.getStatus())) {
            cashDifference = closingCash - expectedCash;
        }

        // 3. Orders stats
        List<OrderEntity> orders = orderRepository.findByCreatedShiftId(shiftId);
        int totalOrders = orders.size();

        int unpaidOrders = 0;
        for (OrderEntity o : orders) {
            String oStatus = o.getStatus();
            if (!"PAID".equalsIgnoreCase(oStatus) && !"CANCELLED".equalsIgnoreCase(oStatus)) {
                unpaidOrders++;
            }
        }

        // 4. Staff & Attendance
        List<AttendanceEntity> attendances = attendanceRepository.findByShiftId(shiftId);
        int staffPresentCount = 0;
        List<ShiftReportResponse.StaffAttendanceSummary> attendanceList = new ArrayList<>();

        for (AttendanceEntity att : attendances) {
            if (att.getCheckInAt() != null && att.getCheckInAt() > 0) {
                staffPresentCount++;
            }
            UserEntity staff = userRepository.findById(att.getUserId()).orElse(null);
            String username = staff != null ? staff.getUsername() : "Unknown";
            String fullName = staff != null ? staff.getFullName() : "Unknown";

            attendanceList.add(new ShiftReportResponse.StaffAttendanceSummary(
                    att.getUserId(),
                    username,
                    fullName,
                    att.getCheckInAt(),
                    att.getCheckOutAt(),
                    att.getStatus(),
                    att.getNotes()
            ));
        }

        // 5. Top products sold (programmatic aggregation)
        Map<Integer, ShiftReportResponse.ProductSoldSummary> productSummaryMap = new HashMap<>();
        for (OrderEntity o : orders) {
            if ("PAID".equalsIgnoreCase(o.getStatus())) {
                List<OrderItemEntity> items = orderItemRepository.findByOrderId(o.getOrderId());
                for (OrderItemEntity item : items) {
                    int pId = item.getProductId();
                    String pName = item.getProductNameSnapshot();
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    double sub = item.getSubtotal() != null ? item.getSubtotal() : 0.0;

                    ShiftReportResponse.ProductSoldSummary prodSummary = productSummaryMap.get(pId);
                    if (prodSummary == null) {
                        prodSummary = new ShiftReportResponse.ProductSoldSummary(pId, pName, 0, 0.0);
                    }
                    prodSummary.setQuantity(prodSummary.getQuantity() + qty);
                    prodSummary.setSubtotal(prodSummary.getSubtotal() + sub);
                    productSummaryMap.put(pId, prodSummary);
                }
            }
        }

        List<ShiftReportResponse.ProductSoldSummary> topProducts = new ArrayList<>(productSummaryMap.values());
        topProducts.sort((a, b) -> b.getQuantity().compareTo(a.getQuantity()));

        // 6. Assigned Staff
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findByShiftId(shiftId);
        List<UserProfileResponse> assignedStaff = assignments.stream()
                .map(a -> {
                    UserEntity user = userRepository.findById(a.getUserId()).orElse(null);
                    if (user != null) {
                        return new UserProfileResponse(
                                user.getUserId(),
                                user.getUsername(),
                                user.getFullName(),
                                user.getRole()
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ShiftReportResponse(
                shift.getShiftId(),
                shift.getShiftName(),
                shift.getShiftDate(),
                shift.getStatus(),
                shift.getOpeningCash(),
                shift.getClosingCash(),
                totalOrders,
                totalRevenue,
                assignedStaff,
                cashRevenue,
                transferRevenue,
                momoRevenue,
                unpaidOrders,
                paymentCount,
                expectedCash,
                cashDifference,
                staffPresentCount,
                paymentMethodStats,
                topProducts,
                attendanceList
        );
    }
}
