package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.SchedulingRequest;
import com.example.cafe_manager_api.dto.SchedulingResponse;
import com.example.cafe_manager_api.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/scheduling")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class SchedulingController {

    @Autowired
    private SchedulingService schedulingService;

    @PostMapping("/preview")
    public SchedulingResponse preview(@Valid @RequestBody SchedulingRequest request, Principal principal) {
        return schedulingService.runPreview(request.getStartDate(), request.getEndDate(), principal.getName());
    }

    @GetMapping("/preview/{runId}")
    public SchedulingResponse getPreview(@PathVariable Long runId) {
        return schedulingService.getPreview(runId);
    }

    @PostMapping("/preview/{runId}/apply")
    public SchedulingResponse applyPreview(@PathVariable Long runId, Principal principal) {
        return schedulingService.applyPreview(runId, principal.getName());
    }
}
