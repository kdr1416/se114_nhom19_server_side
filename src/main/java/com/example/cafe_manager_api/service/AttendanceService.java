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

        if (!Boolean.TRUE.equals(assignment.getConfirmed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn cần xác nhận ca làm việc trước khi check-in.");
        }

        AttendanceEntity existing = attendanceRepository.findByShiftIdAndUserId(request.getShiftId(), userId);

        long checkInTime = System.currentTimeMillis();
        long shiftStartTime = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());

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
}
