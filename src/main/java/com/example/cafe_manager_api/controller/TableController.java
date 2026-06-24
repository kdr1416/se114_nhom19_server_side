package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.TableRequest;
import com.example.cafe_manager_api.dto.TableResponse;
import com.example.cafe_manager_api.service.TableService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tables")
public class TableController {

    @Autowired
    private TableService tableService;

    @GetMapping
    public ResponseEntity<List<TableResponse>> getAllTables(@RequestParam(required = false) String status) {
        List<TableResponse> responses = tableService.getAllTables(status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableResponse> getTableById(@PathVariable Integer id) {
        TableResponse response = tableService.getTableById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    // @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // TODO: Bật lại sau khi di trú AuthRepository
    public ResponseEntity<TableResponse> createTable(@Valid @RequestBody TableRequest request) {
        TableResponse response = tableService.createTable(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // TODO: Bật lại sau khi di trú AuthRepository
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable Integer id,
            @Valid @RequestBody TableRequest request) {
        TableResponse response = tableService.updateTable(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')") // TODO: Bật lại sau khi di trú AuthRepository
    public ResponseEntity<?> deleteTable(@PathVariable Integer id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(Map.of("message", "Xóa bàn thành công."));
    }
}
