package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.CreateAuditLogRequest;
import com.example.cafe_manager_api.entity.AuditLogEntity;
import com.example.cafe_manager_api.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogEntity>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/my")
    public ResponseEntity<List<AuditLogEntity>> getMyLogs(Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        return ResponseEntity.ok(auditLogService.getMyLogs(username));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogEntity> getLogById(@PathVariable Integer id) {
        return ResponseEntity.ok(auditLogService.getLogById(id));
    }

    @PostMapping
    public ResponseEntity<AuditLogEntity> createLog(
            @Valid @RequestBody CreateAuditLogRequest request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        return ResponseEntity.ok(auditLogService.createLog(username, request));
    }
}
