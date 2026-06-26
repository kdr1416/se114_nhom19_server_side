package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.LeaveRequestCreateRequest;
import com.example.cafe_manager_api.dto.LeaveRequestResponse;
import com.example.cafe_manager_api.dto.LeaveReviewRequest;
import com.example.cafe_manager_api.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> submitLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateRequest request,
            Principal principal) {
        LeaveRequestResponse response = leaveRequestService.submitLeaveRequest(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests(Principal principal) {
        List<LeaveRequestResponse> responses = leaveRequestService.getMyLeaveRequests(principal.getName());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequests(
            @RequestParam(required = false) String status,
            Principal principal) {
        List<LeaveRequestResponse> responses = leaveRequestService.getLeaveRequestsForManager(principal.getName(), status);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveReviewRequest reviewRequest,
            Principal principal) {
        String reviewNote = reviewRequest != null ? reviewRequest.getReviewNote() : "";
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(id, reviewNote, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveReviewRequest reviewRequest,
            Principal principal) {
        String reviewNote = reviewRequest != null ? reviewRequest.getReviewNote() : "";
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(id, reviewNote, principal.getName());
        return ResponseEntity.ok(response);
    }
}
