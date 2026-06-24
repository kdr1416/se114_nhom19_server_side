package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.ShiftTemplateRequest;
import com.example.cafe_manager_api.dto.ShiftTemplateResponse;
import com.example.cafe_manager_api.service.ShiftTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shift-templates")
public class ShiftTemplateController {

    @Autowired
    private ShiftTemplateService shiftTemplateService;

    @GetMapping
    public ResponseEntity<List<ShiftTemplateResponse>> getAllTemplates() {
        List<ShiftTemplateResponse> responses = shiftTemplateService.getAllTemplates();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftTemplateResponse> getTemplateById(@PathVariable Integer id) {
        ShiftTemplateResponse response = shiftTemplateService.getTemplateById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftTemplateResponse> createTemplate(@Valid @RequestBody ShiftTemplateRequest request) {
        ShiftTemplateResponse response = shiftTemplateService.createTemplate(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftTemplateResponse> updateTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody ShiftTemplateRequest request) {
        ShiftTemplateResponse response = shiftTemplateService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteTemplate(@PathVariable Integer id) {
        shiftTemplateService.deleteTemplate(id);
        return ResponseEntity.ok(Map.of("message", "Xóa ca mẫu thành công."));
    }
}
