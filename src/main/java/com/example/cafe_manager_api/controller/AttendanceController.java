package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.AttendanceResponse;
import com.example.cafe_manager_api.dto.CheckInRequest;
import com.example.cafe_manager_api.dto.CheckOutRequest;
import com.example.cafe_manager_api.dto.TeamAttendanceSummary;
import com.example.cafe_manager_api.dto.UserAttendanceDetailResponse;
import com.example.cafe_manager_api.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> getAllForUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        List<AttendanceResponse> attendances = attendanceService.getAllAttendancesForUser(principal.getName());
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> getById(@PathVariable Integer id, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        AttendanceResponse attendance = attendanceService.getAttendanceById(id, principal.getName());
        return ResponseEntity.ok(attendance);
    }

    @PostMapping("/checkin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        AttendanceResponse attendance = attendanceService.checkIn(request, principal.getName());
        return ResponseEntity.ok(attendance);
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkOut(@Valid @RequestBody CheckOutRequest request, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        AttendanceResponse attendance = attendanceService.checkOut(request, principal.getName());
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<AttendanceResponse>> getForShift(@PathVariable Integer shiftId, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        List<AttendanceResponse> attendances = attendanceService.getAttendanceForShift(shiftId);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/report/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<TeamAttendanceSummary>> getTeamReport(
            @RequestParam int year,
            @RequestParam int month,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        List<TeamAttendanceSummary> report = attendanceService.getTeamAttendanceReport(year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/report/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserAttendanceDetailResponse> getUserDetails(
            @RequestParam int userId,
            @RequestParam int year,
            @RequestParam int month,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        UserAttendanceDetailResponse details = attendanceService.getUserAttendanceDetails(userId, year, month);
        return ResponseEntity.ok(details);
    }
}
