package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.ConflictResponse;
import com.example.cafe_manager_api.dto.ShiftTemplateResponse;
import com.example.cafe_manager_api.entity.EmployeeWeeklyAvailabilityEntity;
import com.example.cafe_manager_api.entity.ShiftAssignmentEntity;
import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.entity.ShiftTemplateEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.exception.ConflictException;
import com.example.cafe_manager_api.repository.AvailabilityWeekLockRepository;
import com.example.cafe_manager_api.repository.EmployeeAvailabilityRepository;
import com.example.cafe_manager_api.repository.ShiftAssignmentRepository;
import com.example.cafe_manager_api.repository.ShiftRepository;
import com.example.cafe_manager_api.repository.ShiftTemplateRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeAvailabilityService {

    @Autowired
    private EmployeeAvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private AvailabilityWeekLockRepository weekLockRepository;

    // Get active/effective shift templates
    @Transactional(readOnly = true)
    public List<ShiftTemplateResponse> getActiveShiftTemplates() {
        long now = System.currentTimeMillis();
        List<ShiftTemplateEntity> activeTemplates = shiftTemplateRepository.findActiveTemplates(now);
        return activeTemplates.stream()
                .map(t -> new ShiftTemplateResponse(
                        t.getTemplateId(),
                        t.getTemplateName(),
                        t.getStartTime(),
                        t.getEndTime(),
                        t.getMinStaff(),
                        t.getIsActive(),
                        t.getEffectiveFromDate(),
                        t.getEffectiveToDate(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // Get all availability for current user
    @Transactional(readOnly = true)
    public List<EmployeeWeeklyAvailabilityEntity> getMyAvailability(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return availabilityRepository.findByUserId(user.getUserId());
    }

    // Upsert availability (insert or update)
    // If record exists for userId+templateId+dayOfWeek → update isAvailable
    // If not → insert new
    @Transactional
    public EmployeeWeeklyAvailabilityEntity setAvailability(
            Integer templateId, Integer dayOfWeek,
            Boolean isAvailable, String username) {

        // Verify user can only modify own availability (unless ADMIN/MANAGER)
        UserEntity caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Integer userId = caller.getUserId();

        // Week lock check: staff cannot modify availability in locked weeks
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(caller.getRole()) || "MANAGER".equalsIgnoreCase(caller.getRole());
        if (!isPrivileged) {
            Long currentWeekStart = getWeekStart(System.currentTimeMillis());
            if (weekLockRepository.existsByWeekStart(currentWeekStart)) {
                throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Không thể thay đổi sẵn sàng cho tuần này vì đã có ca được phân bổ.");
            }
        }

        // Validate that the template exists and is active/effective
        ShiftTemplateEntity template = shiftTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift template not found"));

        long now = System.currentTimeMillis();
        boolean isActive = Boolean.TRUE.equals(template.getIsActive()) &&
                (template.getEffectiveFromDate() == null || template.getEffectiveFromDate() <= now) &&
                (template.getEffectiveToDate() == null || template.getEffectiveToDate() >= now);

        if (!isActive) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift template is inactive or expired");
        }

        // Conflict check: prevent availability modification if user has future PUBLISHED/OPEN shifts for this template/day
        long todayMidnight = getTodayMidnight();
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findByUserId(userId);
        List<ConflictResponse.ConflictingShiftDTO> conflicts = new ArrayList<>();

        for (ShiftAssignmentEntity assignment : assignments) {
            ShiftEntity shift = shiftRepository.findById(assignment.getShiftId()).orElse(null);
            if (shift == null) {
                continue;
            }

            // Only future shifts (shiftDate >= today midnight)
            if (shift.getShiftDate() < todayMidnight) {
                continue;
            }

            // Only shifts in PUBLISHED or OPEN status
            String shiftStatus = shift.getStatus();
            if (!"PUBLISHED".equalsIgnoreCase(shiftStatus) && !"OPEN".equalsIgnoreCase(shiftStatus)) {
                continue;
            }

            // Template must match
            if (!shift.getTemplateId().equals(templateId)) {
                continue;
            }

            // Day of week must match
            int shiftDayOfWeek = getIsoDayOfWeek(shift.getShiftDate());
            if (shiftDayOfWeek != dayOfWeek) {
                continue;
            }

            // This is a conflicting shift
            conflicts.add(new ConflictResponse.ConflictingShiftDTO(
                    shift.getShiftId(),
                    shift.getShiftDate(),
                    template.getTemplateName(),
                    shiftStatus
            ));
        }

        if (!conflicts.isEmpty()) {
            throw new ConflictException("Bạn đang được xếp vào các ca sau", conflicts);
        }

        Optional<EmployeeWeeklyAvailabilityEntity> existing =
            availabilityRepository.findByUserIdAndTemplateIdAndDayOfWeek(userId, templateId, dayOfWeek);

        if (existing.isPresent()) {
            EmployeeWeeklyAvailabilityEntity record = existing.get();
            record.setIsAvailable(isAvailable);
            record.setUpdatedAt(now);
            return availabilityRepository.save(record);
        } else {
            EmployeeWeeklyAvailabilityEntity record = new EmployeeWeeklyAvailabilityEntity();
            record.setUserId(userId);
            record.setTemplateId(templateId);
            record.setDayOfWeek(dayOfWeek);
            record.setIsAvailable(isAvailable);
            record.setCreatedAt(now);
            record.setUpdatedAt(null);
            // effective dates left null for now
            return availabilityRepository.save(record);
        }
    }

    // Delete availability record
    @Transactional
    public void removeAvailability(Integer availabilityId, String username) {
        // Validate: user can only delete own records (unless ADMIN/MANAGER)
        EmployeeWeeklyAvailabilityEntity record = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found"));

        UserEntity caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        boolean isOwner = caller.getUserId().equals(record.getUserId());
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(caller.getRole()) || "MANAGER".equalsIgnoreCase(caller.getRole());

        if (!isOwner && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this availability record");
        }

        availabilityRepository.delete(record);
    }

    @Transactional
    public List<EmployeeWeeklyAvailabilityEntity> publishAvailability(
            Integer userId, Integer templateId, Integer dayOfWeek, Boolean isAvailable,
            String scope, Long untilDate, String username) {

        // Authorization: user can only publish for themselves (unless ADMIN/MANAGER)
        UserEntity caller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        boolean isPrivileged = "ADMIN".equalsIgnoreCase(caller.getRole()) || "MANAGER".equalsIgnoreCase(caller.getRole());
        if (!caller.getUserId().equals(userId) && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to publish availability for this user");
        }

        // Validate scope
        if (!"THIS_WEEK".equalsIgnoreCase(scope) && !"UNTIL_DATE".equalsIgnoreCase(scope)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be 'THIS_WEEK' or 'UNTIL_DATE'");
        }

        // If UNTIL_DATE, validate untilDate
        if ("UNTIL_DATE".equalsIgnoreCase(scope)) {
            if (untilDate == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "untilDate is required for scope UNTIL_DATE");
            }
            if (untilDate < System.currentTimeMillis()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "untilDate must be in the future");
            }
        }

        List<EmployeeWeeklyAvailabilityEntity> results = new ArrayList<>();

        // Determine weekStarts to publish
        long now = System.currentTimeMillis();
        Long currentWeekStart = getWeekStart(now);
        List<Long> weekStarts = new ArrayList<>();
        if ("THIS_WEEK".equalsIgnoreCase(scope)) {
            weekStarts.add(currentWeekStart);
        } else { // UNTIL_DATE
            Long untilWeekStart = getWeekStart(untilDate);
            for (Long ws = currentWeekStart; ws <= untilWeekStart; ws += 7L * 24 * 60 * 60 * 1000) {
                weekStarts.add(ws);
            }
        }

        // Process each week
        for (Long weekStart : weekStarts) {
            // Skip locked weeks silently for staff; admin/manager can bypass
            if (!isPrivileged && weekLockRepository.existsByWeekStart(weekStart)) {
                continue;
            }

            Optional<EmployeeWeeklyAvailabilityEntity> existing =
                availabilityRepository.findByUserIdAndTemplateIdAndDayOfWeekAndWeekStart(
                    userId, templateId, dayOfWeek, weekStart);

            EmployeeWeeklyAvailabilityEntity record;
            if (existing.isPresent()) {
                record = existing.get();
                record.setIsAvailable(isAvailable);
                record.setStatus("PUBLISHED");
                record.setPublishedUntil("UNTIL_DATE".equalsIgnoreCase(scope) ? untilDate : null);
                record.setUpdatedAt(now);
            } else {
                record = new EmployeeWeeklyAvailabilityEntity();
                record.setUserId(userId);
                record.setTemplateId(templateId);
                record.setDayOfWeek(dayOfWeek);
                record.setIsAvailable(isAvailable);
                record.setCreatedAt(now);
                record.setUpdatedAt(null);
                record.setEffectiveFromDate(null);
                record.setEffectiveToDate(null);
                record.setWeekStart(weekStart);
                record.setStatus("PUBLISHED");
                record.setPublishedUntil("UNTIL_DATE".equalsIgnoreCase(scope) ? untilDate : null);
            }

            EmployeeWeeklyAvailabilityEntity saved = availabilityRepository.save(record);
            results.add(saved);
        }

        return results;
    }

    // Get available staff for a shift (used later by scheduling engine)
    public List<UserEntity> getAvailableStaffForShift(
            Integer templateId, Integer dayOfWeek, Long shiftDate) {

        if (shiftDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shiftDate is required and cannot be null");
        }

        Long weekStart = getWeekStart(shiftDate);
        List<EmployeeWeeklyAvailabilityEntity> availabilities =
            availabilityRepository.findAvailableForShift(templateId, dayOfWeek, shiftDate, weekStart);

        // Extract userIds and fetch UserEntity for each
        // Return only active users
        List<UserEntity> staff = new ArrayList<>();
        for (EmployeeWeeklyAvailabilityEntity avail : availabilities) {
            userRepository.findById(avail.getUserId())
                .ifPresent(user -> {
                    if (Boolean.TRUE.equals(user.getIsActive())) {
                        staff.add(user);
                    }
                });
        }
        return staff;
    }

    // Helper: get today at 00:00:00.000 in epoch millis
    private long getTodayMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Helper: convert epoch millis to ISO day of week (1=Monday, 7=Sunday)
    private Integer getIsoDayOfWeek(Long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        int calendarDay = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 2=Monday, ...
        // Convert to ISO: Monday=1, Sunday=7
        // Calendar: Sunday=1, Monday=2, ..., Saturday=7
        // ISO: Monday=1 -> Calendar.MONDAY(2) => 1, Sunday=7 => Calendar.SUNDAY(1) => 7
        return (calendarDay + 5) % 7 + 1;
    }

    // Compute weekStart (Monday 00:00 Asia/Ho_Chi_Minh) for an epoch millis
    private Long getWeekStart(Long epochMillis) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate();
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        return monday.atStartOfDay(zone).toInstant().toEpochMilli();
    }
}
