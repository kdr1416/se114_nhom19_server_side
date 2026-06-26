package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.AttendanceResponse;
import com.example.cafe_manager_api.dto.CheckInRequest;
import com.example.cafe_manager_api.dto.CheckOutRequest;
import com.example.cafe_manager_api.dto.UpdateAttendanceRequest;
import com.example.cafe_manager_api.entity.AttendanceEntity;
import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.entity.ShiftAssignmentEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.AttendanceRepository;
import com.example.cafe_manager_api.repository.ShiftAssignmentRepository;
import com.example.cafe_manager_api.repository.ShiftRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.util.Constants;
import com.example.cafe_manager_api.util.ShiftTimeUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.cafe_manager_api.repository.OrderRepository orderRepository;

    @Autowired
    private com.example.cafe_manager_api.repository.PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAllAttendancesForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));
        Integer userId = user.getUserId();
        List<AttendanceEntity> attendances = attendanceRepository.findByUserId(userId);
        return attendances.stream()
                .map(a -> mapToResponse(a, username, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendanceById(int attendanceId, String username) {
        AttendanceEntity attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new EntityNotFoundException("Bản ghi điểm danh không tồn tại."));
        UserEntity user = userRepository.findById(attendance.getUserId())
                .orElse(null);
        String userName = user != null ? user.getUsername() : null;
        return mapToResponse(attendance, userName, attendance.getUserId());
    }

    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));
        Integer userId = user.getUserId();

        ShiftEntity shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new EntityNotFoundException("Ca làm việc không tồn tại."));

        if (Constants.SHIFT_CANCELLED.equals(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ca làm việc này đã bị hủy.");
        }

        if (!Constants.SHIFT_PUBLISHED.equals(shift.getStatus()) &&
            !Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể check-in cho ca đã phát hành hoặc đang chạy.");
        }

        ShiftAssignmentEntity assignment = shiftAssignmentRepository
                .findByShiftIdAndUserId(request.getShiftId(), userId)
                .orElse(null);
        if (assignment == null) {
            throw new AccessDeniedException("Bạn không được phân công ca làm việc này.");
        }

        long checkInTime = System.currentTimeMillis();
        long shiftStartTime = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());

        if (checkInTime < shiftStartTime - 15 * 60 * 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa đến giờ check-in (chỉ được check-in trước giờ bắt đầu ca tối đa 15 phút).");
        }

        AttendanceEntity existing = attendanceRepository.findByShiftIdAndUserId(request.getShiftId(), userId);

        String status = Constants.ATTENDANCE_CHECKED_IN;
        int lateMin = 0;
        if (checkInTime > shiftStartTime + (15 * 60 * 1000)) {
            status = Constants.ATTENDANCE_LATE;
        }
        if (checkInTime > shiftStartTime) {
            lateMin = (int) ((checkInTime - shiftStartTime) / 60000);
        }

        AttendanceEntity saved;
        if (existing == null) {
            AttendanceEntity attendance = new AttendanceEntity();
            attendance.setShiftId(request.getShiftId());
            attendance.setUserId(userId);
            attendance.setCheckInAt(checkInTime);
            attendance.setLateMinutes(lateMin);
            attendance.setStatus(status);
            saved = attendanceRepository.save(attendance);
        } else {
            if (existing.getCheckInAt() != null && existing.getCheckInAt() > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã check-in ca này rồi.");
            }
            existing.setCheckInAt(checkInTime);
            existing.setLateMinutes(lateMin);
            existing.setStatus(status);
            saved = attendanceRepository.save(existing);
        }

        return mapToResponse(saved, username, userId);
    }

    @Transactional
    public AttendanceResponse checkOut(CheckOutRequest request, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));
        Integer userId = user.getUserId();

        AttendanceEntity attendance = attendanceRepository
                .findByShiftIdAndUserId(request.getShiftId(), userId);

        if (attendance == null) {
            throw new EntityNotFoundException("Không tìm thấy bản ghi check-in cho ca làm việc này.");
        }

        if (attendance.getCheckInAt() == null || attendance.getCheckInAt() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn phải check-in trước khi check-out.");
        }

        if (attendance.getCheckOutAt() != null && attendance.getCheckOutAt() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã check-out ca này rồi.");
        }

        ShiftEntity shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new EntityNotFoundException("Ca làm việc không tồn tại."));

        long checkOutTime = System.currentTimeMillis();
        long shiftStartTime = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());
        long shiftEndTime = ShiftTimeUtils.getShiftEndMillis(shift.getShiftDate(), shift.getStartTime(), shift.getEndTime());

        int lateMin = (attendance.getCheckInAt() > shiftStartTime) ? (int) ((attendance.getCheckInAt() - shiftStartTime) / 60000) : 0;
        int earlyLeaveMin = (checkOutTime < shiftEndTime) ? (int) ((shiftEndTime - checkOutTime) / 60000) : 0;

        String status = Constants.ATTENDANCE_COMPLETED;
        if (lateMin > 15) {
            status = Constants.ATTENDANCE_LATE;
        }
        if (earlyLeaveMin > 15) {
            status = Constants.ATTENDANCE_EARLY_LEAVE;
        }

        attendance.setCheckOutAt(checkOutTime);
        attendance.setLateMinutes(lateMin);
        attendance.setEarlyLeaveMinutes(earlyLeaveMin);
        attendance.setStatus(status);
        attendance.setNotes(request.getNotes());
        attendanceRepository.save(attendance);

        return mapToResponse(attendance, username, userId);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceForShift(int shiftId) {
        List<AttendanceEntity> attendances = attendanceRepository.findByShiftId(shiftId);
        if (attendances.isEmpty()) {
            return new ArrayList<>();
        }
        // Batch fetch usernames
        Set<Integer> userIds = attendances.stream()
                .map(AttendanceEntity::getUserId)
                .collect(Collectors.toSet());
        List<UserEntity> users = userRepository.findAllByUserIdIn(new ArrayList<>(userIds));
        Map<Integer, String> usernameMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getUsername));

        return attendances.stream()
                .map(a -> mapToResponse(a, usernameMap.getOrDefault(a.getUserId(), ""), a.getUserId()))
                .collect(Collectors.toList());
    }

    private AttendanceResponse mapToResponse(AttendanceEntity entity, String username, Integer userId) {
        AttendanceResponse r = new AttendanceResponse();
        r.setAttendanceId(entity.getAttendanceId());
        r.setShiftId(entity.getShiftId());
        r.setUserId(userId);
        r.setUsername(username);
        r.setCheckInAt(entity.getCheckInAt());
        r.setCheckOutAt(entity.getCheckOutAt());
        r.setStatus(entity.getStatus());
        r.setLateMinutes(entity.getLateMinutes());
        r.setEarlyLeaveMinutes(entity.getEarlyLeaveMinutes());
        r.setNotes(entity.getNotes());
        return r;
    }

    @Transactional(readOnly = true)
    public List<com.example.cafe_manager_api.dto.TeamAttendanceSummary> getTeamAttendanceReport(int year, int month) {
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.LocalDate start = java.time.LocalDate.of(year, month, 1);
        long startEpoch = start.atStartOfDay(zone).toInstant().toEpochMilli();
        long endEpoch = start.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;

        List<ShiftEntity> shifts = shiftRepository.findShiftsInRange(startEpoch, endEpoch);
        List<ShiftEntity> activeShifts = shifts.stream()
                .filter(s -> !Constants.SHIFT_CANCELLED.equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        if (activeShifts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> shiftIds = activeShifts.stream().map(ShiftEntity::getShiftId).collect(Collectors.toList());
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findAssignmentsInDateRange(startEpoch, endEpoch);
        
        List<AttendanceEntity> attendances = attendanceRepository.findByShiftIdIn(shiftIds);
        Map<String, AttendanceEntity> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(a -> a.getUserId() + "_" + a.getShiftId(), a -> a, (a1, a2) -> a1));

        Map<Integer, ShiftEntity> shiftMap = activeShifts.stream()
                .collect(Collectors.toMap(ShiftEntity::getShiftId, s -> s));

        List<UserEntity> allUsers = userRepository.findAll();
        List<com.example.cafe_manager_api.dto.TeamAttendanceSummary> summaries = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (UserEntity user : allUsers) {
            List<ShiftAssignmentEntity> userAssignments = assignments.stream()
                    .filter(a -> a.getUserId().equals(user.getUserId()) && shiftMap.containsKey(a.getShiftId()))
                    .collect(Collectors.toList());

            if (userAssignments.isEmpty() && !Boolean.TRUE.equals(user.getIsActive())) {
                continue; // Skip inactive users who have no assignments this month
            }

            int totalShifts = userAssignments.size();
            int attendedShifts = 0;
            int absentShifts = 0;
            int lateCount = 0;
            int earlyLeaveCount = 0;
            double totalHoursWorked = 0.0;

            for (ShiftAssignmentEntity assignment : userAssignments) {
                ShiftEntity shift = shiftMap.get(assignment.getShiftId());
                AttendanceEntity att = attendanceMap.get(user.getUserId() + "_" + assignment.getShiftId());

                long shiftStart = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());
                long shiftEnd = ShiftTimeUtils.getShiftEndMillis(shift.getShiftDate(), shift.getStartTime(), shift.getEndTime());

                if (att != null && att.getCheckInAt() != null && att.getCheckInAt() > 0) {
                    attendedShifts++;
                    if (att.getLateMinutes() != null && att.getLateMinutes() > 0) {
                        lateCount++;
                    }
                    if (att.getEarlyLeaveMinutes() != null && att.getEarlyLeaveMinutes() > 0) {
                        earlyLeaveCount++;
                    }
                    if (att.getCheckOutAt() != null && att.getCheckOutAt() > 0) {
                        long effectiveStart = Math.max(att.getCheckInAt(), shiftStart);
                        long effectiveEnd = Math.min(att.getCheckOutAt(), shiftEnd);
                        double hours = Math.max(0.0, (effectiveEnd - effectiveStart) / 3600000.0);
                        totalHoursWorked += hours;
                    }
                } else {
                    if (shiftEnd < now) {
                        absentShifts++;
                    }
                }
            }

            double attendanceRate = totalShifts > 0 ? (attendedShifts * 100.0 / totalShifts) : 0.0;
            attendanceRate = Math.round(attendanceRate * 10.0) / 10.0;
            totalHoursWorked = Math.round(totalHoursWorked * 10.0) / 10.0;

            int ordersCreated = 0;
            int paymentsProcessed = 0;
            double revenueProcessed = 0.0;
            List<Integer> userShiftIds = userAssignments.stream()
                    .map(ShiftAssignmentEntity::getShiftId)
                    .collect(Collectors.toList());
            if (!userShiftIds.isEmpty()) {
                ordersCreated = (int) orderRepository.countCreatedOrdersByShiftIds(user.getUserId(), userShiftIds);
                paymentsProcessed = (int) paymentRepository.countProcessedPaymentsByShiftIds(user.getUserId(), userShiftIds);
                revenueProcessed = paymentRepository.sumRevenueByShiftIds(user.getUserId(), userShiftIds);
            }

            summaries.add(new com.example.cafe_manager_api.dto.TeamAttendanceSummary(
                    user.getUserId(),
                    user.getFullName(),
                    user.getUsername(),
                    user.getRole(),
                    totalShifts,
                    attendedShifts,
                    absentShifts,
                    lateCount,
                    earlyLeaveCount,
                    totalHoursWorked,
                    attendanceRate,
                    ordersCreated,
                    paymentsProcessed,
                    revenueProcessed
            ));
        }

        summaries.sort((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()));
        return summaries;
    }

    @Transactional(readOnly = true)
    public com.example.cafe_manager_api.dto.UserAttendanceDetailResponse getUserAttendanceDetails(int userId, int year, int month) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Nhân viên không tồn tại."));

        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.LocalDate start = java.time.LocalDate.of(year, month, 1);
        long startEpoch = start.atStartOfDay(zone).toInstant().toEpochMilli();
        long endEpoch = start.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;

        List<ShiftEntity> shifts = shiftRepository.findShiftsInRange(startEpoch, endEpoch);
        List<ShiftEntity> activeShifts = shifts.stream()
                .filter(s -> !Constants.SHIFT_CANCELLED.equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());

        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findAssignmentsInDateRange(startEpoch, endEpoch)
                .stream()
                .filter(a -> a.getUserId().equals(userId))
                .collect(Collectors.toList());

        List<Integer> shiftIds = activeShifts.stream().map(ShiftEntity::getShiftId).collect(Collectors.toList());
        List<AttendanceEntity> attendances = attendanceRepository.findByShiftIdIn(shiftIds).stream()
                .filter(a -> a.getUserId().equals(userId))
                .collect(Collectors.toList());

        Map<Integer, AttendanceEntity> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(AttendanceEntity::getShiftId, a -> a, (a1, a2) -> a1));

        Map<Integer, ShiftEntity> shiftMap = activeShifts.stream()
                .collect(Collectors.toMap(ShiftEntity::getShiftId, s -> s));

        List<com.example.cafe_manager_api.dto.UserAttendanceDetailResponse.AttendanceRecord> records = new ArrayList<>();
        long now = System.currentTimeMillis();

        int totalShifts = 0;
        int attendedShifts = 0;
        int absentShifts = 0;
        int lateCount = 0;
        int earlyLeaveCount = 0;
        double totalHoursWorked = 0.0;

        for (ShiftAssignmentEntity assignment : assignments) {
            ShiftEntity shift = shiftMap.get(assignment.getShiftId());
            if (shift == null) continue;

            totalShifts++;
            AttendanceEntity att = attendanceMap.get(shift.getShiftId());

            long shiftStart = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());
            long shiftEnd = ShiftTimeUtils.getShiftEndMillis(shift.getShiftDate(), shift.getStartTime(), shift.getEndTime());
            double durationHours = Math.round(((shiftEnd - shiftStart) / 3600000.0) * 10.0) / 10.0;

            Long checkInAt = null;
            Long checkOutAt = null;
            int lateMin = 0;
            int earlyLeaveMin = 0;
            String recordStatus = "UPCOMING";
            String notes = "";

            if (att != null) {
                checkInAt = att.getCheckInAt();
                checkOutAt = att.getCheckOutAt();
                lateMin = att.getLateMinutes() != null ? att.getLateMinutes() : 0;
                earlyLeaveMin = att.getEarlyLeaveMinutes() != null ? att.getEarlyLeaveMinutes() : 0;
                notes = att.getNotes() != null ? att.getNotes() : "";

                if (checkInAt != null && checkInAt > 0) {
                    attendedShifts++;
                    if (lateMin > 0) {
                        lateCount++;
                    }
                    if (earlyLeaveMin > 0) {
                        earlyLeaveCount++;
                    }
                    if (checkOutAt != null && checkOutAt > 0) {
                        long effectiveStart = Math.max(checkInAt, shiftStart);
                        long effectiveEnd = Math.min(checkOutAt, shiftEnd);
                        double hours = Math.max(0.0, (effectiveEnd - effectiveStart) / 3600000.0);
                        totalHoursWorked += hours;
                        recordStatus = att.getStatus();
                    } else {
                        recordStatus = "IN_PROGRESS";
                    }
                } else {
                    if (shiftEnd < now) {
                        absentShifts++;
                        recordStatus = "ABSENT";
                    } else if (shiftStart < now) {
                        recordStatus = "IN_PROGRESS";
                    }
                }
            } else {
                if (shiftEnd < now) {
                    absentShifts++;
                    recordStatus = "ABSENT";
                } else if (shiftStart < now) {
                    recordStatus = "IN_PROGRESS";
                }
            }

            int shiftOrders = (int) orderRepository.countCreatedOrdersByShiftId(userId, shift.getShiftId());
            int shiftPayments = (int) paymentRepository.countProcessedPaymentsByShiftId(userId, shift.getShiftId());
            double shiftRevenue = paymentRepository.sumRevenueByShiftId(userId, shift.getShiftId());

            records.add(new com.example.cafe_manager_api.dto.UserAttendanceDetailResponse.AttendanceRecord(
                    shift.getShiftId(),
                    shift.getShiftName(),
                    shift.getShiftDate(),
                    shift.getStartTime(),
                    shift.getEndTime(),
                    durationHours,
                    checkInAt != null ? checkInAt : 0L,
                    checkOutAt != null ? checkOutAt : 0L,
                    lateMin,
                    earlyLeaveMin,
                    recordStatus,
                    notes,
                    shiftOrders,
                    shiftPayments,
                    shiftRevenue
            ));
        }

        records.sort((a, b) -> {
            int cmp = Long.compare(a.getShiftDate(), b.getShiftDate());
            if (cmp != 0) return cmp;
            return a.getStartTime().compareTo(b.getStartTime());
        });

        double attendanceRate = totalShifts > 0 ? (attendedShifts * 100.0 / totalShifts) : 0.0;
        attendanceRate = Math.round(attendanceRate * 10.0) / 10.0;
        totalHoursWorked = Math.round(totalHoursWorked * 10.0) / 10.0;

        int monthOrders = 0;
        int monthPayments = 0;
        double monthRevenue = 0.0;
        List<Integer> allShiftIds = assignments.stream()
                .map(ShiftAssignmentEntity::getShiftId)
                .collect(Collectors.toList());
        if (!allShiftIds.isEmpty()) {
            monthOrders = (int) orderRepository.countCreatedOrdersByShiftIds(userId, allShiftIds);
            monthPayments = (int) paymentRepository.countProcessedPaymentsByShiftIds(userId, allShiftIds);
            monthRevenue = paymentRepository.sumRevenueByShiftIds(userId, allShiftIds);
        }

        return new com.example.cafe_manager_api.dto.UserAttendanceDetailResponse(
                user.getUserId(),
                user.getFullName(),
                user.getUsername(),
                user.getRole(),
                totalShifts,
                attendedShifts,
                absentShifts,
                lateCount,
                earlyLeaveCount,
                totalHoursWorked,
                attendanceRate,
                monthOrders,
                monthPayments,
                monthRevenue,
                records
        );
    }
}
