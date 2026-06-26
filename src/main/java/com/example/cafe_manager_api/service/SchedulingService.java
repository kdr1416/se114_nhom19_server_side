package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.ShiftSuggestion;
import com.example.cafe_manager_api.dto.SchedulingResponse;
import com.example.cafe_manager_api.entity.EmployeeWeeklyAvailabilityEntity;
import com.example.cafe_manager_api.entity.SchedulingRunEntity;
import com.example.cafe_manager_api.entity.ShiftAssignmentEntity;
import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.entity.ShiftTemplateEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.entity.AvailabilityWeekLockEntity;
import com.example.cafe_manager_api.repository.EmployeeAvailabilityRepository;
import com.example.cafe_manager_api.repository.LeaveRequestRepository;
import com.example.cafe_manager_api.repository.SchedulingRunRepository;
import com.example.cafe_manager_api.repository.ShiftAssignmentRepository;
import com.example.cafe_manager_api.repository.ShiftRepository;
import com.example.cafe_manager_api.repository.ShiftTemplateRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.repository.AvailabilityWeekLockRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class SchedulingService {

    @Autowired
    private SchedulingEngine schedulingEngine;

    @Autowired
    private SchedulingRunRepository schedulingRunRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvailabilityWeekLockRepository availabilityWeekLockRepository;

    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;

    private final ObjectMapper objectMapper;

    public SchedulingService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional
    public SchedulingResponse runPreview(Long startDate, Long endDate, String username) {
        // Auto-create DRAFT shifts from active templates if none exist
        List<ShiftEntity> existingDrafts = shiftRepository.findDraftShiftsInRange(startDate, endDate);
        if (existingDrafts.isEmpty()) {
            long now = System.currentTimeMillis();
            List<ShiftTemplateEntity> activeTemplates = shiftTemplateRepository.findActiveTemplates(now);
            long dayMs = 24L * 60 * 60 * 1000;
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (long day = startDate; day <= endDate; day += dayMs) {
                LocalDate date = Instant.ofEpochMilli(day).atZone(ZONE_ID).toLocalDate();
                for (ShiftTemplateEntity tmpl : activeTemplates) {
                    ShiftEntity shift = new ShiftEntity();
                    shift.setTemplateId(tmpl.getTemplateId());
                    shift.setShiftName(tmpl.getTemplateName() + " - " + date.format(fmt));
                    shift.setShiftDate(day);
                    shift.setStartTime(tmpl.getStartTime());
                    shift.setEndTime(tmpl.getEndTime());
                    shift.setStatus("DRAFT");
                    shift.setCreatedAt(now);
                    shiftRepository.save(shift);
                }
            }
        }

        // Generate suggestions
        List<ShiftSuggestion> suggestions = schedulingEngine.generateSuggestions(startDate, endDate);

        // Serialize suggestions to JSON
        String suggestionsJson;
        try {
            suggestionsJson = objectMapper.writeValueAsString(suggestions);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize scheduling suggestions", e);
        }

        // Get manager user ID
        UserEntity manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Integer managerId = manager.getUserId();

        // Create and save SchedulingRunEntity
        SchedulingRunEntity run = new SchedulingRunEntity();
        run.setStartDate(startDate);
        run.setEndDate(endDate);
        run.setStatus("PREVIEW");
        run.setCreatedBy(managerId);
        run.setCreatedAt(System.currentTimeMillis());
        run.setAppliedAt(null);
        run.setSuggestionsJson(suggestionsJson);

        SchedulingRunEntity saved = schedulingRunRepository.save(run);

        // Auto-lock all weeks in the scheduling date range
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Long w = getWeekStart(startDate, zone);
        Long endWeekStart = getWeekStart(endDate, zone);
        Long nowTs = System.currentTimeMillis();
        Integer lockingManagerId = saved.getCreatedBy();

        while (w <= endWeekStart) {
            if (!availabilityWeekLockRepository.existsByWeekStart(w)) {
                availabilityWeekLockRepository.save(
                    new AvailabilityWeekLockEntity(w, nowTs, lockingManagerId));
            }
            w += 7L * 24 * 60 * 60 * 1000;
        }

        // Build response
        return buildResponse(saved, suggestions);
    }

    @Transactional(readOnly = true)
    public SchedulingResponse getPreview(Long runId) {
        SchedulingRunEntity run = schedulingRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduling run not found"));

        // Deserialize suggestionsJson
        List<ShiftSuggestion> suggestions;
        try {
            suggestions = objectMapper.readValue(
                    run.getSuggestionsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ShiftSuggestion.class)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize scheduling suggestions", e);
        }

        return buildResponse(run, suggestions);
    }

    @Transactional
    public SchedulingResponse applyPreview(Long runId, String username) {
        // Load run
        SchedulingRunEntity run = schedulingRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduling run not found"));

        if (!"PREVIEW".equals(run.getStatus())) {
            throw new IllegalArgumentException("Only PREVIEW runs can be applied");
        }

        // Get manager user ID
        UserEntity manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Integer managerId = manager.getUserId();

        // Deserialize suggestions
        List<ShiftSuggestion> suggestions;
        try {
            suggestions = objectMapper.readValue(
                    run.getSuggestionsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ShiftSuggestion.class)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize scheduling suggestions", e);
        }

        // Pre-load all users for role lookup (avoid N+1 queries)
        java.util.Map<Integer, String> userRoleMap = new java.util.HashMap<>();
        for (UserEntity u : userRepository.findAll()) {
            userRoleMap.put(u.getUserId(), u.getRole());
        }

        // Pre-load existing assignments for duplicate check (avoid N queries)
        java.util.Set<String> existingAssignmentKeys = new java.util.HashSet<>();
        for (ShiftSuggestion suggestion : suggestions) {
            List<ShiftAssignmentEntity> existing = shiftAssignmentRepository.findByShiftId(suggestion.getShiftId());
            for (ShiftAssignmentEntity a : existing) {
                existingAssignmentKeys.add(a.getShiftId() + "_" + a.getUserId());
            }
        }

        // Build all assignments in memory, then batch save
        List<ShiftAssignmentEntity> toSave = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (ShiftSuggestion suggestion : suggestions) {
            for (Integer userId : suggestion.getSuggestedUserIds()) {
                String key = suggestion.getShiftId() + "_" + userId;
                if (existingAssignmentKeys.contains(key)) {
                    continue; // Skip duplicate
                }
                existingAssignmentKeys.add(key);

                ShiftAssignmentEntity assignment = new ShiftAssignmentEntity();
                assignment.setShiftId(suggestion.getShiftId());
                assignment.setUserId(userId);
                assignment.setRole(userRoleMap.getOrDefault(userId, "STAFF"));
                assignment.setAssignedBy(managerId);
                assignment.setConfirmed(false);
                assignment.setCreatedAt(now);

                toSave.add(assignment);
            }
        }

        // Batch save all assignments at once
        if (!toSave.isEmpty()) {
            shiftAssignmentRepository.saveAll(toSave);
        }

        // Update run status
        run.setStatus("APPLIED");
        run.setAppliedAt(now);
        schedulingRunRepository.save(run);

        // Return updated response
        return buildResponse(run, suggestions);
    }

    private SchedulingResponse buildResponse(SchedulingRunEntity run, List<ShiftSuggestion> suggestions) {
        int total = suggestions.size();
        int fulfilled = 0;
        int missing = 0;
        for (ShiftSuggestion s : suggestions) {
            if (s.getIsFulfilled()) {
                fulfilled++;
            } else {
                missing += s.getMissingCount();
            }
        }

        return new SchedulingResponse(
                run.getRunId(),
                run.getStartDate(),
                run.getEndDate(),
                run.getStatus(),
                run.getCreatedAt(),
                run.getAppliedAt(),
                suggestions,
                total,
                fulfilled,
                missing
        );
    }

    // Compute weekStart (Monday 00:00) for an epoch millis in given zone
    private Long getWeekStart(Long epochMillis, ZoneId zone) {
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate();
        return date.with(java.time.DayOfWeek.MONDAY)
                   .atStartOfDay(zone).toInstant().toEpochMilli();
    }
}
