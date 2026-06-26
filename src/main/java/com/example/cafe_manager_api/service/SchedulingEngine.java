package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.ShiftSuggestion;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.entity.ShiftAssignmentEntity;
import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.entity.ShiftTemplateEntity;
import com.example.cafe_manager_api.entity.LeaveRequestEntity;
import com.example.cafe_manager_api.entity.EmployeeWeeklyAvailabilityEntity;
import com.example.cafe_manager_api.repository.EmployeeAvailabilityRepository;
import com.example.cafe_manager_api.repository.LeaveRequestRepository;
import com.example.cafe_manager_api.repository.ShiftAssignmentRepository;
import com.example.cafe_manager_api.repository.ShiftRepository;
import com.example.cafe_manager_api.repository.ShiftTemplateRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class SchedulingEngine {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;

    @Autowired
    private EmployeeAvailabilityRepository employeeAvailabilityRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    public List<ShiftSuggestion> generateSuggestions(Long startDate, Long endDate) {
        List<ShiftSuggestion> suggestions = new ArrayList<>();

        // STEP 1: Load all DRAFT shifts in the date range
        List<ShiftEntity> draftShifts = shiftRepository.findDraftShiftsInRange(startDate, endDate);

        if (draftShifts.isEmpty()) {
            return suggestions;
        }

        // Load ALL shifts in the date range (needed for assignment overlap checks)
        List<ShiftEntity> allShiftsInRange = shiftRepository.findShiftsInRange(startDate, endDate);
        HashMap<Integer, ShiftEntity> shiftMap = new HashMap<>();
        for (ShiftEntity s : allShiftsInRange) {
            shiftMap.put(s.getShiftId(), s);
        }

        // STEP 2: All needed template IDs
        List<Integer> templateIds = draftShifts.stream()
                .map(ShiftEntity::getTemplateId)
                .distinct()
                .toList();
        List<ShiftTemplateEntity> templates = shiftTemplateRepository.findAllById(templateIds);
        HashMap<Integer, ShiftTemplateEntity> templateMap = new HashMap<>();
        for (ShiftTemplateEntity t : templates) {
            templateMap.put(t.getTemplateId(), t);
        }

        // STEP 3: All active users (for availability lookup)
        List<UserEntity> allUsers = userRepository.findAll();
        HashMap<Integer, UserEntity> userMap = new HashMap<>();
        for (UserEntity u : allUsers) {
            if (Boolean.TRUE.equals(u.getIsActive())) {
                userMap.put(u.getUserId(), u);
            }
        }

        // STEP 4: All approved leaves overlapping [startDate, endDate]
        List<LeaveRequestEntity> approvedLeaves = leaveRequestRepository.findApprovedLeavesInRange(startDate, endDate);
        HashMap<Integer, List<LeaveRequestEntity>> leavesByUser = new HashMap<>();
        for (LeaveRequestEntity l : approvedLeaves) {
            leavesByUser
                .computeIfAbsent(l.getUserId(), k -> new ArrayList<>())
                .add(l);
        }

        // STEP 5: All existing shift assignments for the date range (non-cancelled shifts)
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findAssignmentsInDateRange(startDate, endDate);
        HashMap<Integer, List<ShiftAssignmentEntity>> assignmentsByUser = new HashMap<>();
        for (ShiftAssignmentEntity a : assignments) {
            assignmentsByUser
                .computeIfAbsent(a.getUserId(), k -> new ArrayList<>())
                .add(a);
        }

        // BATCH LOAD 6: All availability records for all templates in range (1 query total)
        List<EmployeeWeeklyAvailabilityEntity> allAvailabilities =
                employeeAvailabilityRepository.findAvailableForTemplates(
                        new ArrayList<>(templateIds), startDate, endDate);

        // Group availability records by templateId + "_" + dayOfWeek
        // Key: templateId + "_" + dayOfWeek
        java.util.Map<String, List<EmployeeWeeklyAvailabilityEntity>> availabilityMap = new java.util.HashMap<>();
        for (EmployeeWeeklyAvailabilityEntity a : allAvailabilities) {
            if (!"PUBLISHED".equals(a.getStatus())) continue;
            String key = a.getTemplateId() + "_" + a.getDayOfWeek();
            availabilityMap
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(a);
        }

        // STEP 6: Track assignment count per user for load balancing
        HashMap<Integer, Integer> assignmentCountByUser = new HashMap<>();
        // Initialize with existing assignment counts from pre-loaded data
        for (var entry : assignmentsByUser.entrySet()) {
            assignmentCountByUser.put(entry.getKey(), entry.getValue().size());
        }

        // Process each shift using in-memory lookups
        for (ShiftEntity shift : draftShifts) {
            Integer shiftId = shift.getShiftId();
            Long shiftDate = shift.getShiftDate();
            String startTime = shift.getStartTime();
            String endTime = shift.getEndTime();
            Integer templateId = shift.getTemplateId();

            // Get template from map
            ShiftTemplateEntity template = templateMap.get(templateId);
            if (template == null || !template.getIsActive()) {
                continue;
            }

            Integer minStaff = template.getMinStaff();

            // Derive dayOfWeek from shiftDate
            LocalDate date = Instant.ofEpochMilli(shiftDate).atZone(ZONE_ID).toLocalDate();
            int dayOfWeek = date.getDayOfWeek().getValue();

            // Get weekStart for availability check
            Long weekStart = getWeekStart(shiftDate);

            List<Integer> suggestedUserIds = new ArrayList<>();
            List<String> suggestedUserNames = new ArrayList<>();

            long shiftStartEpoch = parseShiftEpoch(shiftDate, startTime);
            long shiftEndEpoch = parseShiftEpoch(shiftDate, endTime);

            // Filter candidates in-memory matching the database query constraints
            String availKey = templateId + "_" + dayOfWeek;
            List<EmployeeWeeklyAvailabilityEntity> candidates = new ArrayList<>(
                availabilityMap.getOrDefault(availKey, java.util.Collections.emptyList()));

            // Sort candidates by assignment count (ascending) for fair distribution
            candidates.sort((a1, a2) -> {
                int count1 = assignmentCountByUser.getOrDefault(a1.getUserId(), 0);
                int count2 = assignmentCountByUser.getOrDefault(a2.getUserId(), 0);
                return Integer.compare(count1, count2);
            });

            for (EmployeeWeeklyAvailabilityEntity a : candidates) {
                // Check weekStart (must be null or equal to current weekStart)
                if (a.getWeekStart() != null && !a.getWeekStart().equals(weekStart)) {
                    continue;
                }
                // Check effective date range
                if (a.getEffectiveFromDate() != null && a.getEffectiveFromDate() > shiftDate) {
                    continue;
                }
                if (a.getEffectiveToDate() != null && a.getEffectiveToDate() < shiftDate) {
                    continue;
                }

                UserEntity staff = userMap.get(a.getUserId());
                if (staff == null || !Boolean.TRUE.equals(staff.getIsActive())) {
                    continue;
                }

                // Skip ADMIN and MANAGER from staffing suggestions
                String role = staff.getRole();
                if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
                    continue;
                }

                Integer userId = staff.getUserId();

                // 1. Check for APPROVED leave overlap (using pre-loaded leaves)
                List<LeaveRequestEntity> userLeaves = leavesByUser.getOrDefault(userId, List.of());
                boolean hasLeave = false;
                for (LeaveRequestEntity leave : userLeaves) {
                    if (leave.getStartAt() < shiftEndEpoch && leave.getEndAt() > shiftStartEpoch) {
                        hasLeave = true;
                        break;
                    }
                }
                if (hasLeave) {
                    continue;
                }

                // 2. Check for existing overlapping shift assignments (using pre-loaded assignments)
                List<ShiftAssignmentEntity> userAssignments = assignmentsByUser.getOrDefault(userId, List.of());
                boolean hasOverlap = false;
                for (ShiftAssignmentEntity assignment : userAssignments) {
                    ShiftEntity assignedShift = shiftMap.get(assignment.getShiftId());
                    if (assignedShift == null) {
                        continue;
                    }
                    // Skip checking the current shift itself
                    if (shiftId.equals(assignedShift.getShiftId())) {
                        continue;
                    }
                    long assignedStart = parseShiftEpoch(assignedShift.getShiftDate(), assignedShift.getStartTime());
                    long assignedEnd = parseShiftEpoch(assignedShift.getShiftDate(), assignedShift.getEndTime());
                    if (shiftStartEpoch < assignedEnd && shiftEndEpoch > assignedStart) {
                        hasOverlap = true;
                        break;
                    }
                }
                if (hasOverlap) {
                    continue;
                }

                // Candidate accepted
                suggestedUserIds.add(userId);
                suggestedUserNames.add(staff.getFullName());
                assignmentCountByUser.merge(userId, 1, Integer::sum);

                if (suggestedUserIds.size() >= minStaff) {
                    break;
                }
            }

            // Build suggestion
            ShiftSuggestion suggestion = new ShiftSuggestion();
            suggestion.setShiftId(shift.getShiftId());
            suggestion.setShiftDate(shiftDate);
            suggestion.setTemplateId(templateId);
            suggestion.setTemplateName(template.getTemplateName());
            suggestion.setStartTime(startTime);
            suggestion.setEndTime(endTime);
            suggestion.setMinStaff(minStaff);
            suggestion.setSuggestedUserIds(suggestedUserIds);
            suggestion.setSuggestedUserNames(suggestedUserNames);
            suggestion.setIsFulfilled(suggestedUserIds.size() >= minStaff);
            suggestion.setMissingCount(Math.max(0, minStaff - suggestedUserIds.size()));

            suggestions.add(suggestion);
        }

        return suggestions;
    }

    private Long getWeekStart(Long epochMillis) {
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(ZONE_ID).toLocalDate();
        return date.with(java.time.DayOfWeek.MONDAY)
                   .atStartOfDay(ZONE_ID).toInstant().toEpochMilli();
    }

    private long parseShiftEpoch(Long shiftDate, String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            LocalDate date = Instant.ofEpochMilli(shiftDate).atZone(ZONE_ID).toLocalDate();
            return date.atTime(hour, minute)
                    .atZone(ZONE_ID)
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception e) {
            return shiftDate;
        }
    }
}
