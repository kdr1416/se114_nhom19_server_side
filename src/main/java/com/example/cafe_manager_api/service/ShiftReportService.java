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
import java.time.LocalDate;
import java.time.ZoneId;

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

    @Autowired
    private TableRepository tableRepository;

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
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findByShiftId(shiftId);

        // Bulk Load Users
        Set<Integer> userIds = new HashSet<>();
        for (AttendanceEntity att : attendances) {
            userIds.add(att.getUserId());
        }
        for (ShiftAssignmentEntity a : assignments) {
            userIds.add(a.getUserId());
        }
        
        List<Integer> orderIds = orders.stream().map(OrderEntity::getOrderId).collect(Collectors.toList());
        List<PaymentEntity> shiftOrderPayments = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            shiftOrderPayments = paymentRepository.findByOrderIdIn(orderIds);
        }
        Map<Integer, PaymentEntity> paymentByOrderId = shiftOrderPayments.stream()
                .collect(Collectors.toMap(PaymentEntity::getOrderId, p -> p, (p1, p2) -> p1));

        for (PaymentEntity p : shiftOrderPayments) {
            if (p.getCashierUserId() != null) {
                userIds.add(p.getCashierUserId());
            }
        }

        Map<Integer, UserEntity> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserEntity> users = userRepository.findAllByUserIdIn(new ArrayList<>(userIds));
            for (UserEntity u : users) {
                userMap.put(u.getUserId(), u);
            }
        }

        // Bulk Load Tables
        Set<Integer> tableIds = new HashSet<>();
        for (OrderEntity o : orders) {
            if (o.getTableId() != null) {
                tableIds.add(o.getTableId());
            }
        }
        Map<Integer, TableEntity> tableMap = new HashMap<>();
        if (!tableIds.isEmpty()) {
            List<TableEntity> tables = tableRepository.findAllById(tableIds);
            for (TableEntity t : tables) {
                tableMap.put(t.getTableId(), t);
            }
        }

        // Build Attendance List
        int staffPresentCount = 0;
        List<ShiftReportResponse.StaffAttendanceSummary> attendanceList = new ArrayList<>();
        for (AttendanceEntity att : attendances) {
            if (att.getCheckInAt() != null && att.getCheckInAt() > 0) {
                staffPresentCount++;
            }
            UserEntity staff = userMap.get(att.getUserId());
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

        // 5. Top products sold (programmatic aggregation with bulk loading)
        List<Integer> paidOrderIds = orders.stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .map(OrderEntity::getOrderId)
                .collect(Collectors.toList());

        List<OrderItemEntity> allItems = new ArrayList<>();
        if (!paidOrderIds.isEmpty()) {
            allItems = orderItemRepository.findByOrderIdIn(paidOrderIds);
        }
        Map<Integer, List<OrderItemEntity>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        Map<Integer, ShiftReportResponse.ProductSoldSummary> productSummaryMap = new HashMap<>();
        for (OrderEntity o : orders) {
            if ("PAID".equalsIgnoreCase(o.getStatus())) {
                List<OrderItemEntity> items = itemsByOrderId.getOrDefault(o.getOrderId(), Collections.emptyList());
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
        List<UserProfileResponse> assignedStaff = assignments.stream()
                .map(a -> {
                    UserEntity user = userMap.get(a.getUserId());
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

        // 7. Order History
        List<ShiftReportResponse.ShiftOrderResponse> orderHistory = new ArrayList<>();
        for (OrderEntity o : orders) {
            String tableName = "Bàn Mang Đi";
            if (o.getTableId() != null) {
                TableEntity table = tableMap.get(o.getTableId());
                if (table != null) {
                    tableName = table.getTableName();
                }
            }

            String paymentMethod = "—";
            Long paidAt = null;
            String cashierName = "—";

            PaymentEntity p = paymentByOrderId.get(o.getOrderId());
            if (p != null) {
                paymentMethod = p.getPaymentMethod();
                paidAt = p.getPaidAt();
                if (p.getCashierUserId() != null) {
                    UserEntity cashier = userMap.get(p.getCashierUserId());
                    if (cashier != null) {
                        cashierName = cashier.getFullName();
                    }
                }
            }

            orderHistory.add(new ShiftReportResponse.ShiftOrderResponse(
                    o.getOrderId(),
                    o.getOrderCode(),
                    tableName,
                    o.getTotalAmount(),
                    o.getStatus(),
                    paymentMethod,
                    paidAt,
                    cashierName
            ));
        }

        orderHistory.sort((a, b) -> {
            Long timeA = a.getPaidAt() != null ? a.getPaidAt() : (a.getOrderId() != null ? a.getOrderId().longValue() : 0L);
            Long timeB = b.getPaidAt() != null ? b.getPaidAt() : (b.getOrderId() != null ? b.getOrderId().longValue() : 0L);
            return timeB.compareTo(timeA);
        });

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
                attendanceList,
                orderHistory
        );
    }

    @Transactional(readOnly = true)
    public DailyShiftReportResponse getDailyShiftReport(String dateStr) {
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày không hợp lệ. Sử dụng yyyy-MM-dd.");
        }
        
        long shiftDate = localDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();
        List<ShiftEntity> shifts = shiftRepository.filterShifts(shiftDate, null);
        
        double dailyTotalRevenue = 0.0;
        int dailyTotalOrders = 0;
        int dailyPaymentCount = 0;
        double dailyTotalOpeningCash = 0.0;
        double dailyTotalExpectedCash = 0.0;
        
        List<DailyShiftReportResponse.ShiftSummary> shiftSummaries = new ArrayList<>();
        
        if (!shifts.isEmpty()) {
            List<Integer> shiftIds = shifts.stream().map(ShiftEntity::getShiftId).collect(Collectors.toList());
            
            List<PaymentEntity> allPayments = paymentRepository.findByPaidShiftIdInAndStatus(shiftIds, "PAID");
            List<OrderEntity> allOrders = orderRepository.findByCreatedShiftIdIn(shiftIds);
            
            Map<Integer, List<PaymentEntity>> paymentsByShift = allPayments.stream()
                    .collect(Collectors.groupingBy(p -> p.getPaidShiftId() != null ? p.getPaidShiftId() : -1));
            
            Map<Integer, List<OrderEntity>> ordersByShift = allOrders.stream()
                    .collect(Collectors.groupingBy(o -> o.getCreatedShiftId() != null ? o.getCreatedShiftId() : -1));
            
            for (ShiftEntity shift : shifts) {
                int shiftId = shift.getShiftId();
                List<PaymentEntity> shiftPayments = paymentsByShift.getOrDefault(shiftId, Collections.emptyList());
                List<OrderEntity> shiftOrders = ordersByShift.getOrDefault(shiftId, Collections.emptyList());
                
                double shiftRevenue = 0.0;
                double shiftCashRevenue = 0.0;
                for (PaymentEntity p : shiftPayments) {
                    double amount = p.getFinalAmount() != null ? p.getFinalAmount() : 0.0;
                    shiftRevenue += amount;
                    if ("CASH".equalsIgnoreCase(p.getPaymentMethod())) {
                        shiftCashRevenue += amount;
                    }
                }
                
                double openingCash = shift.getOpeningCash() != null ? shift.getOpeningCash() : 0.0;
                double expectedCash = openingCash + shiftCashRevenue;
                
                dailyTotalRevenue += shiftRevenue;
                dailyTotalOrders += shiftOrders.size();
                dailyPaymentCount += shiftPayments.size();
                dailyTotalOpeningCash += openingCash;
                dailyTotalExpectedCash += expectedCash;
                
                shiftSummaries.add(new DailyShiftReportResponse.ShiftSummary(
                        shift.getShiftId(),
                        shift.getShiftName(),
                        shift.getStartTime(),
                        shift.getEndTime(),
                        shift.getStatus(),
                        shiftRevenue,
                        shiftOrders.size(),
                        openingCash,
                        expectedCash
                ));
            }
        }
        
        return new DailyShiftReportResponse(
                dateStr,
                dailyTotalRevenue,
                dailyTotalOrders,
                dailyPaymentCount,
                dailyTotalOpeningCash,
                dailyTotalExpectedCash,
                shiftSummaries
        );
    }
}
