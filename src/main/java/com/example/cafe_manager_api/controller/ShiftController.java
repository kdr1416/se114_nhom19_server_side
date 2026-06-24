package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.ShiftRequest;
import com.example.cafe_manager_api.dto.ShiftResponse;
import com.example.cafe_manager_api.dto.ShiftReportResponse;
import com.example.cafe_manager_api.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @GetMapping
    public ResponseEntity<List<ShiftResponse>> getShifts(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        List<ShiftResponse> responses = shiftService.getShifts(date, status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> getShiftById(@PathVariable Integer id) {
        ShiftResponse response = shiftService.getShiftById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftResponse> createShift(@Valid @RequestBody ShiftRequest request) {
        ShiftResponse response = shiftService.createShift(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftResponse> publishShift(@PathVariable Integer id) {
        ShiftResponse response = shiftService.publishShift(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/open")
    public ResponseEntity<ShiftResponse> openShift(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Double> body,
            @RequestParam(required = false) Double openingCash,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        Double cash = null;
        if (body != null && body.containsKey("openingCash")) {
            cash = body.get("openingCash");
        } else if (openingCash != null) {
            cash = openingCash;
        }
        if (cash == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu cung cấp số tiền mở ca (openingCash).");
        }
        ShiftResponse response = shiftService.openShift(id, principal.getName(), cash);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ShiftResponse> closeShift(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Double> body,
            @RequestParam(required = false) Double closingCash,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        Double cash = null;
        if (body != null && body.containsKey("closingCash")) {
            cash = body.get("closingCash");
        } else if (closingCash != null) {
            cash = closingCash;
        }
        if (cash == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu cung cấp số tiền đóng ca (closingCash).");
        }
        ShiftResponse response = shiftService.closeShift(id, principal.getName(), cash);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftResponse> cancelShift(@PathVariable Integer id) {
        ShiftResponse response = shiftService.cancelShift(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> assignStaff(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Integer> body,
            @RequestParam(required = false) Integer userId,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa xác thực.");
        }
        Integer uId = null;
        if (body != null && body.containsKey("userId")) {
            uId = body.get("userId");
        } else if (userId != null) {
            uId = userId;
        }
        if (uId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu cung cấp userId trong body hoặc query param.");
        }
        shiftService.assignStaff(id, uId, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Phân công nhân viên thành công."));
    }

    @DeleteMapping("/{id}/assign/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> unassignStaff(
            @PathVariable Integer id,
            @PathVariable Integer userId) {
        shiftService.unassignStaff(id, userId);
        return ResponseEntity.ok(Map.of("message", "Hủy phân công nhân viên thành công."));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftReportResponse> getShiftReport(@PathVariable Integer id) {
        ShiftReportResponse response = shiftService.getShiftReport(id);
        return ResponseEntity.ok(response);
    }
}
