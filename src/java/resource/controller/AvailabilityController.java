package com.islandtrails.booking.controller;

import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.service.AvailabilityService;
import com.islandtrails.resource.service.ConflictDetectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final ConflictDetectionService conflictDetectionService;

    // Injects availability and conflict detection services
    public AvailabilityController(AvailabilityService availabilityService, ConflictDetectionService conflictDetectionService) {
        this.availabilityService = availabilityService;
        this.conflictDetectionService = conflictDetectionService;
    }

    // Checks if a specific resource is free or returns all available resources for a date range
    @GetMapping("/api/staff/tour-ops/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "resourceId", required = false) Long resourceId) {

        // Step 1: Put the requested dates in the response map
        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDate);
        response.put("endDate", endDate);

        // Step 2: Check conflict for a specific resource or query all available resources
        if (resourceId != null) {
            boolean hasConflict = conflictDetectionService.hasConflict(resourceId, startDate, endDate);
            response.put("resourceId", resourceId);
            response.put("hasConflict", hasConflict);
            response.put("isAvailable", !hasConflict);
        } else {
            List<Resource> available = availabilityService.getAvailableResources(startDate, endDate);
            response.put("availableResources", available);
            response.put("availableCount", available.size());
        }

        // Step 3: Return the response as JSON
        return ResponseEntity.ok(response);
    }
}
