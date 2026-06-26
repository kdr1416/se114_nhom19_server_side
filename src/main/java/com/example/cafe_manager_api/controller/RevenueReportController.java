package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.RevenueReportResponse;
import com.example.cafe_manager_api.service.RevenueReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/reports/revenue")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class RevenueReportController {

    @Autowired
    private RevenueReportService revenueReportService;

    @GetMapping
    public ResponseEntity<RevenueReportResponse> getMonthlyRevenue(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month. Month must be between 1 and 12.");
        }
        if (year < 2020) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year. Year must be 2020 or later.");
        }
        RevenueReportResponse response = revenueReportService.getMonthlyRevenue(year, month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<RevenueReportResponse> getYearlySummary(
            @RequestParam("year") int year) {
        if (year < 2020) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year. Year must be 2020 or later.");
        }
        RevenueReportResponse response = revenueReportService.getYearlySummary(year);
        return ResponseEntity.ok(response);
    }
}
