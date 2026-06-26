package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.ConflictResponse;
import com.example.cafe_manager_api.dto.PublishAvailabilityRequest;
import com.example.cafe_manager_api.dto.SetAvailabilityRequest;
import com.example.cafe_manager_api.dto.ShiftTemplateResponse;
import com.example.cafe_manager_api.dto.WeekLockResponse;
import com.example.cafe_manager_api.entity.AvailabilityWeekLockEntity;
import com.example.cafe_manager_api.entity.EmployeeWeeklyAvailabilityEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.exception.ConflictException;
import com.example.cafe_manager_api.repository.AvailabilityWeekLockRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.service.EmployeeAvailabilityService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
public class EmployeeAvailabilityController {

    @Autowired
    private EmployeeAvailabilityService availabilityService;

    @Autowired
    private AvailabilityWeekLockRepository weekLockRepository;

    @Autowired
    private UserRepository userRepository;

    // GET /api/v1/availability/shift-templates/active - Get active shift templates
    @GetMapping("/shift-templates/active")
    public ResponseEntity<List<ShiftTemplateResponse>> getActiveShiftTemplates() {
        List<ShiftTemplateResponse> templates = availabilityService.getActiveShiftTemplates();
        return ResponseEntity.ok(templates);
    }

    // GET /api/v1/availability/me - Get current user's availability
    @GetMapping("/me")
    public ResponseEntity<List<EmployeeWeeklyAvailabilityEntity>> getMyAvailability() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        List<EmployeeWeeklyAvailabilityEntity> availability = availabilityService.getMyAvailability(username);
        return ResponseEntity.ok(availability);
    }

    // POST /api/v1/availability - Create or update availability
    @PostMapping
    public ResponseEntity<Object> setAvailability(
            @Valid @RequestBody SetAvailabilityRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        try {
            EmployeeWeeklyAvailabilityEntity result = availabilityService.setAvailability(
                    request.getTemplateId(),
                    request.getDayOfWeek(),
                    request.getIsAvailable(),
                    username
            );
            return ResponseEntity.ok(result);
        } catch (ConflictException e) {
            ConflictResponse body = e.getConflictResponse();
            return new ResponseEntity<>(body, HttpStatus.CONFLICT);
        }
    }

    // DELETE /api/v1/availability/{id} - Remove availability record
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeAvailability(@PathVariable Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        availabilityService.removeAvailability(id, username);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/availability/publish - Publish availability for specific weeks
    @PostMapping("/publish")
    public ResponseEntity<List<EmployeeWeeklyAvailabilityEntity>> publishAvailability(
            @Valid @RequestBody PublishAvailabilityRequest request,
            Principal principal) {

        UserEntity user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<EmployeeWeeklyAvailabilityEntity> results = availabilityService.publishAvailability(
                user.getUserId(),
                request.getTemplateId(),
                request.getDayOfWeek(),
                request.getIsAvailable(),
                request.getScope(),
                request.getUntilDate(),
                principal.getName()
        );
        // TODO: Map to AvailabilityResponse DTO when available
        return ResponseEntity.ok(results);
    }

    // GET /api/v1/availability/week-lock - Check if a week is locked
    @GetMapping("/week-lock")
    public ResponseEntity<WeekLockResponse> getWeekLock(@RequestParam Long weekStart) {
        boolean isLocked = weekLockRepository.existsByWeekStart(weekStart);
        if (!isLocked) {
            return ResponseEntity.ok(new WeekLockResponse(false, null, null));
        }
        AvailabilityWeekLockEntity lock = weekLockRepository.findById(weekStart)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lock inconsistency"));
        WeekLockResponse response = new WeekLockResponse(true, lock.getLockedAt(), lock.getLockedBy());
        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/availability/week-lock - Unlock a week (admin/manager only)
    @DeleteMapping("/week-lock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> unlockWeek(@RequestParam Long weekStart) {
        weekLockRepository.deleteById(weekStart);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/availability/staff/available?templateId=X&dayOfWeek=Y&shiftDate=Z
    // Find available staff for scheduling (used by scheduling engine)
    @GetMapping("/staff/available")
    public ResponseEntity<List<UserEntity>> getAvailableStaffForShift(
            @RequestParam Integer templateId,
            @RequestParam Integer dayOfWeek,
            @RequestParam Long shiftDate) {
        List<UserEntity> staff = availabilityService.getAvailableStaffForShift(templateId, dayOfWeek, shiftDate);
        return ResponseEntity.ok(staff);
    }
}
