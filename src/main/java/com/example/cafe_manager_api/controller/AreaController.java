package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.AreaResponse;
import com.example.cafe_manager_api.dto.CreateAreaRequest;
import com.example.cafe_manager_api.dto.UpdateAreaRequest;
import com.example.cafe_manager_api.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

    @Autowired
    private AreaService areaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AreaResponse>> getAllAreas() {
        return ResponseEntity.ok(areaService.getAllAreas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AreaResponse> getAreaById(@PathVariable Integer id) {
        return ResponseEntity.ok(areaService.getAreaById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AreaResponse> createArea(@Valid @RequestBody CreateAreaRequest request) {
        return ResponseEntity.ok(areaService.createArea(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AreaResponse> updateArea(@PathVariable Integer id, @Valid @RequestBody UpdateAreaRequest request) {
        return ResponseEntity.ok(areaService.updateArea(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteArea(@PathVariable Integer id) {
        areaService.deleteArea(id);
        return ResponseEntity.ok().build();
    }
}