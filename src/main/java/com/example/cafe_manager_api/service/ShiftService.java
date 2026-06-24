package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.*;
import com.example.cafe_manager_api.entity.*;
import com.example.cafe_manager_api.exception.OverlapException;
import com.example.cafe_manager_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private ShiftResponse mapToResponse(ShiftEntity shift) {
        return new ShiftResponse(
                shift.getShiftId(),
                shift.getTemplateId(),
                shift.getShiftName(),
                shift.getShiftDate(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getStatus(),
                shift.getOpenedBy(),
                shift.getOpenedAt(),
                shift.getClosedBy(),
                shift.getClosedAt(),
                shift.getOpeningCash(),
                shift.getClosingCash(),
                shift.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getShifts(String dateStr, String status) {
        Long shiftDate = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr.trim());
                shiftDate = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng ngày không hợp lệ. Sử dụng yyyy-MM-dd.");
            }
        }
        
        List<ShiftEntity> shifts = shiftRepository.filterShifts(shiftDate, status);
        return shifts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShiftResponse getShiftById(Integer id) {
        ShiftEntity shift = shiftRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + id));
        return mapToResponse(shift);
    }

    @Transactional
    public ShiftResponse createShift(ShiftRequest request) {
        ShiftEntity shift = new ShiftEntity();
        shift.setTemplateId(request.getTemplateId());
        shift.setShiftName(request.getShiftName().trim());
        shift.setShiftDate(request.getShiftDate());
        shift.setStartTime(request.getStartTime().trim());
        shift.setEndTime(request.getEndTime().trim());
        shift.setStatus("DRAFT");
        shift.setCreatedAt(System.currentTimeMillis());

        ShiftEntity saved = shiftRepository.save(shift);
        return mapToResponse(saved);
    }

    @Transactional
    public ShiftResponse publishShift(Integer shiftId) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        if (!"DRAFT".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ ca làm việc có trạng thái DRAFT mới có thể PUBLISH.");
        }

        shift.setStatus("PUBLISHED");
        ShiftEntity updated = shiftRepository.save(shift);
        return mapToResponse(updated);
    }

    @Transactional
    public ShiftResponse openShift(Integer shiftId, String username, Double openingCash) {
        // 1. Check no other shift is currently IN_PROGRESS
        List<ShiftEntity> activeShifts = shiftRepository.filterShifts(null, "IN_PROGRESS");
        if (!activeShifts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đã có ca khác đang hoạt động (IN_PROGRESS).");
        }

        // 2. Check this shift is in PUBLISHED status
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        if (!"PUBLISHED".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ ca làm việc ở trạng thái PUBLISHED mới có thể mở.");
        }

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        long now = System.currentTimeMillis();

        // 3. Update shift details
        shift.setStatus("IN_PROGRESS");
        shift.setOpenedBy(user.getUserId());
        shift.setOpenedAt(now);
        shift.setOpeningCash(openingCash);
        ShiftEntity saved = shiftRepository.save(shift);

        // 4. Create ChatRoom for this shift if not exists
        ChatRoomEntity chatRoom = chatRoomRepository.findByShiftId(shiftId).orElse(null);
        if (chatRoom == null) {
            chatRoom = new ChatRoomEntity();
            String dateStr = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(shift.getShiftDate()), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            chatRoom.setRoomName(shift.getShiftName() + " - " + dateStr);
            chatRoom.setRoomType("SHIFT");
            chatRoom.setShiftId(shiftId);
            chatRoom.setTargetRole(null);
            chatRoom.setCreatedBy(user.getUserId());
            chatRoom.setCreatedAt(now);
            chatRoom.setUpdatedAt(now);
            chatRoom.setIsActive(true);
            chatRoom = chatRoomRepository.save(chatRoom);
        }

        // 5. Add all assigned staff as ChatParticipants
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findByShiftId(shiftId);
        for (ShiftAssignmentEntity assignment : assignments) {
            Optional<ChatParticipantEntity> participantOpt = chatParticipantRepository
                    .findByRoomIdAndUserId(chatRoom.getRoomId(), assignment.getUserId());
            if (participantOpt.isEmpty()) {
                ChatParticipantEntity participant = new ChatParticipantEntity();
                participant.setRoomId(chatRoom.getRoomId());
                participant.setUserId(assignment.getUserId());
                participant.setJoinedAt(now);
                participant.setLeftAt(null);
                participant.setRoleInRoom("MEMBER");
                chatParticipantRepository.save(participant);
            }
        }

        return mapToResponse(saved);
    }

    @Transactional
    public ShiftResponse closeShift(Integer shiftId, String username, Double closingCash) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        // 1. Verify shift is IN_PROGRESS
        if (!"IN_PROGRESS".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ca làm việc phải ở trạng thái IN_PROGRESS mới có thể đóng.");
        }

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        // 2. Verify userId is the one who opened it (or is ADMIN/MANAGER)
        boolean isOpener = user.getUserId().equals(shift.getOpenedBy());
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(user.getRole()) || "MANAGER".equalsIgnoreCase(user.getRole());
        if (!isOpener && !isPrivileged) {
            throw new AccessDeniedException("Bạn không có quyền đóng ca làm việc này (chỉ người mở ca, Admin hoặc Manager mới có quyền).");
        }

        // 3. Update shift: status=CLOSED, closedBy=userId, closedAt=now, closingCash=cash
        shift.setStatus("CLOSED");
        shift.setClosedBy(user.getUserId());
        shift.setClosedAt(System.currentTimeMillis());
        shift.setClosingCash(closingCash);

        ShiftEntity saved = shiftRepository.save(shift);
        return mapToResponse(saved);
    }

    @Transactional
    public ShiftResponse cancelShift(Integer shiftId) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        if ("CLOSED".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy ca làm việc đã đóng.");
        }
        if ("CANCELLED".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ca làm việc đã được hủy trước đó.");
        }

        shift.setStatus("CANCELLED");
        ShiftEntity saved = shiftRepository.save(shift);
        return mapToResponse(saved);
    }

    @Transactional
    public void assignStaff(Integer shiftId, Integer userId, String requesterUsername) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        UserEntity staff = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với ID: " + userId));

        UserEntity requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người yêu cầu không tồn tại."));

        // Check overlap with SQL (NOT Java loop)
        long overlapCount = shiftAssignmentRepository.countOverlappingShifts(
                userId, 
                shift.getShiftDate(), 
                shift.getStartTime(), 
                shift.getEndTime()
        );

        if (overlapCount > 0) {
            throw new OverlapException("Nhân viên đã bị trùng lịch ở một ca làm việc khác trong ngày.");
        }

        // Check if already assigned
        Optional<ShiftAssignmentEntity> existing = shiftAssignmentRepository.findByShiftIdAndUserId(shiftId, userId);
        if (existing.isPresent()) {
            return; // Already assigned, do nothing
        }

        ShiftAssignmentEntity assignment = new ShiftAssignmentEntity();
        assignment.setShiftId(shiftId);
        assignment.setUserId(userId);
        assignment.setRole(staff.getRole());
        assignment.setAssignedBy(requester.getUserId());
        assignment.setConfirmed(true);
        assignment.setCreatedAt(System.currentTimeMillis());

        shiftAssignmentRepository.save(assignment);
    }

    @Transactional
    public void unassignStaff(Integer shiftId, Integer userId) {
        ShiftAssignmentEntity assignment = shiftAssignmentRepository.findByShiftIdAndUserId(shiftId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phân công của nhân viên này trong ca."));

        shiftAssignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public ShiftReportResponse getShiftReport(Integer shiftId) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca làm việc với ID: " + shiftId));

        // Get orders created in this shift
        List<OrderEntity> orders = orderRepository.findByCreatedShiftId(shiftId);
        int totalOrders = orders.size();
        double totalRevenue = orders.stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(OrderEntity::getTotalAmount)
                .sum();

        // Get assigned staff
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
                .filter(u -> u != null)
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
                assignedStaff
        );
    }
}
