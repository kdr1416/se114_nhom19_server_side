package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.LeaveRequestCreateRequest;
import com.example.cafe_manager_api.dto.LeaveRequestResponse;
import com.example.cafe_manager_api.entity.LeaveRequestEntity;
import com.example.cafe_manager_api.entity.ShiftAssignmentEntity;
import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.EmployeeAvailabilityRepository;
import com.example.cafe_manager_api.repository.LeaveRequestRepository;
import com.example.cafe_manager_api.repository.ShiftAssignmentRepository;
import com.example.cafe_manager_api.repository.ShiftRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.util.ShiftTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Autowired
    private EmployeeAvailabilityRepository employeeAvailabilityRepository;

    private static final String STATUS_PENDING = "PENDING";
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");

    @Transactional
    public LeaveRequestResponse submitLeaveRequest(String username, LeaveRequestCreateRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));

        if (request.getStartAt() == null || request.getEndAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu và kết thúc không được để trống.");
        }

        if (request.getStartAt() >= request.getEndAt()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do xin nghỉ không được để trống.");
        }

        // Check for overlapping PENDING/APPROVED requests
        List<LeaveRequestEntity> overlaps = leaveRequestRepository.findOverlappingRequests(
                user.getUserId(), request.getStartAt(), request.getEndAt());
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã có đơn xin nghỉ ở trạng thái chờ duyệt hoặc đã duyệt trùng với khoảng thời gian này.");
        }

        LeaveRequestEntity entity = new LeaveRequestEntity();
        entity.setUserId(user.getUserId());
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        entity.setReason(request.getReason().trim());
        entity.setStatus(STATUS_PENDING);
        entity.setCreatedAt(System.currentTimeMillis());

        LeaveRequestEntity saved = leaveRequestRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaveRequests(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));

        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeaveRequestsForManager(String username, String status) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));

        String role = user.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách đơn xin nghỉ.");
        }

        List<LeaveRequestEntity> list;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            String cleanStatus = status.trim().toUpperCase();
            if (!VALID_STATUSES.contains(cleanStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái đơn không hợp lệ.");
            }
            list = leaveRequestRepository.findByStatusOrderByCreatedAtDesc(cleanStatus);
        } else {
            list = leaveRequestRepository.findAll().stream()
                    .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                    .collect(Collectors.toList());
        }

        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private LeaveRequestResponse mapToResponse(LeaveRequestEntity entity) {
        LeaveRequestResponse resp = new LeaveRequestResponse();
        resp.setLeaveRequestId(entity.getLeaveRequestId());
        resp.setUserId(entity.getUserId());
        resp.setStartAt(entity.getStartAt());
        resp.setEndAt(entity.getEndAt());
        resp.setReason(entity.getReason());
        resp.setStatus(entity.getStatus());
        resp.setReviewedByUserId(entity.getReviewedByUserId());
        resp.setReviewedAt(entity.getReviewedAt());
        resp.setReviewNote(entity.getReviewNote());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());

        // Fill user name
        userRepository.findById(entity.getUserId())
                .ifPresent(u -> resp.setUserName(u.getFullName()));

        // Fill reviewer name
        if (entity.getReviewedByUserId() != null) {
            userRepository.findById(entity.getReviewedByUserId())
                    .ifPresent(u -> resp.setReviewedByName(u.getFullName()));
        }

        return resp;
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long leaveRequestId, String reviewNote, String managerUsername) {
        UserEntity manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quản lý không tồn tại."));

        String role = manager.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt đơn xin nghỉ.");
        }

        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn xin nghỉ với ID: " + leaveRequestId));

        if (!STATUS_PENDING.equalsIgnoreCase(leaveRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ đơn xin nghỉ ở trạng thái PENDING mới có thể duyệt.");
        }

        Integer staffUserId = leaveRequest.getUserId();
        long leaveStart = leaveRequest.getStartAt();
        long leaveEnd = leaveRequest.getEndAt();

        // Find shift assignments for the staff
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findByUserId(staffUserId);

        int affectedCount = 0;

        if (!assignments.isEmpty()) {
            List<Integer> shiftIds = assignments.stream()
                    .map(ShiftAssignmentEntity::getShiftId)
                    .collect(Collectors.toList());

            List<ShiftEntity> shifts = shiftRepository.findAllById(shiftIds);
            Map<Integer, ShiftEntity> shiftMap = shifts.stream()
                    .collect(Collectors.toMap(ShiftEntity::getShiftId, s -> s));

            for (ShiftAssignmentEntity assignment : assignments) {
                ShiftEntity shift = shiftMap.get(assignment.getShiftId());
                if (shift == null) {
                    continue;
                }

                // Check status
                String shiftStatus = shift.getStatus();
                if ("IN_PROGRESS".equalsIgnoreCase(shiftStatus) ||
                    "CLOSED".equalsIgnoreCase(shiftStatus) ||
                    "CANCELLED".equalsIgnoreCase(shiftStatus)) {
                    continue; // Skip, cannot cancel
                }

                // Calculate timestamps using ShiftTimeUtils
                long shiftStart = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());
                long shiftEnd = ShiftTimeUtils.getShiftEndMillis(shift.getShiftDate(), shift.getStartTime(), shift.getEndTime());

                // Overlap check: shiftStart < leaveEnd && shiftEnd > leaveStart
                if (shiftStart < leaveEnd && shiftEnd > leaveStart) {
                    shiftAssignmentRepository.delete(assignment);

                    // Cleanup: set isAvailable=false for the matching availability slot in that week
                    ShiftEntity shiftObj = shiftRepository.findById(assignment.getShiftId()).orElse(null);
                    if (shiftObj != null) {
                        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
                        LocalDate shiftDate = Instant.ofEpochMilli(shiftObj.getShiftDate())
                            .atZone(zone).toLocalDate();
                        int isoDayOfWeek = shiftDate.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
                        Long weekStart = shiftDate.with(java.time.DayOfWeek.MONDAY)
                            .atStartOfDay(zone).toInstant().toEpochMilli();

                        employeeAvailabilityRepository
                            .findByUserIdAndTemplateIdAndDayOfWeekAndWeekStart(
                                staffUserId,
                                shiftObj.getTemplateId(),
                                isoDayOfWeek,
                                weekStart)
                            .ifPresent(avail -> {
                                avail.setIsAvailable(false);
                                avail.setUpdatedAt(System.currentTimeMillis());
                                employeeAvailabilityRepository.save(avail);
                            });
                    }

                    affectedCount++;
                }
            }
        }

        // Update leave request status
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setReviewedByUserId(manager.getUserId());
        leaveRequest.setReviewedAt(System.currentTimeMillis());
        leaveRequest.setReviewNote(reviewNote != null ? reviewNote.trim() : "");
        leaveRequest.setUpdatedAt(System.currentTimeMillis());

        LeaveRequestEntity saved = leaveRequestRepository.save(leaveRequest);

        // Notification Hook: Log statements
        System.out.println("Notification: Đơn xin nghỉ của nhân viên ID " + staffUserId + " đã được duyệt.");
        if (affectedCount > 0) {
            System.out.println("Notification: Bạn đã được gỡ khỏi " + affectedCount + " ca làm việc do đơn xin nghỉ được duyệt.");
        }

        LeaveRequestResponse response = mapToResponse(saved);
        response.setAffectedAssignmentCount(affectedCount);
        return response;
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long leaveRequestId, String reviewNote, String managerUsername) {
        UserEntity manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quản lý không tồn tại."));

        String role = manager.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền từ chối đơn xin nghỉ.");
        }

        LeaveRequestEntity leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn xin nghỉ với ID: " + leaveRequestId));

        if (!STATUS_PENDING.equalsIgnoreCase(leaveRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ đơn xin nghỉ ở trạng thái PENDING mới có thể từ chối.");
        }

        // Update status
        leaveRequest.setStatus("REJECTED");
        leaveRequest.setReviewedByUserId(manager.getUserId());
        leaveRequest.setReviewedAt(System.currentTimeMillis());
        leaveRequest.setReviewNote(reviewNote != null ? reviewNote.trim() : "");
        leaveRequest.setUpdatedAt(System.currentTimeMillis());

        LeaveRequestEntity saved = leaveRequestRepository.save(leaveRequest);

        // Notification Hook: Log statement
        System.out.println("Notification: Đơn xin nghỉ của nhân viên ID " + leaveRequest.getUserId() + " đã bị từ chối.");

        LeaveRequestResponse response = mapToResponse(saved);
        response.setAffectedAssignmentCount(0);
        return response;
    }
}
