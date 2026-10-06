package com.islandtrails.resource.service;

import com.islandtrails.common.util.ConflictCheckUtil;
import com.islandtrails.resource.entity.ResourceAssignment;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import com.islandtrails.resource.strategy.ResourceAvailabilityStrategy;
import com.islandtrails.resource.strategy.StrictAvailabilityStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConflictDetectionService {

    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final ResourceAvailabilityStrategy strategy;

    // Single constructor dependency injection
    public ConflictDetectionService(ResourceAssignmentRepository resourceAssignmentRepository,
                                  ResourceAvailabilityStrategy strategy) {
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.strategy = strategy != null ? strategy : new StrictAvailabilityStrategy();
    }

    // Checks if two date ranges overlap using the canonical ConflictCheckUtil
    public boolean isOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return ConflictCheckUtil.isOverlapping(start1, end1, start2, end2);
    }

    // Checks if a resource is already booked during the requested dates using the availability strategy
    public boolean hasConflict(Long resourceId, LocalDate startDate, LocalDate endDate) {
        // Step 1: Return false if any required parameter is missing
        if (resourceId == null || startDate == null || endDate == null) {
            return false;
        }

        // Step 2: Fetch active assignments for the resource
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findActiveAssignmentsForResource(resourceId);
        if (assignments == null || assignments.isEmpty()) {
            List<ResourceAssignment> overlapping = resourceAssignmentRepository.findOverlapping(resourceId, startDate, endDate);
            if (overlapping != null && !overlapping.isEmpty()) {
                assignments = overlapping;
            } else if (assignments == null) {
                assignments = Collections.emptyList();
            }
        }

        // Step 3: Evaluate availability via strategy
        return !strategy.isAvailable(assignments, startDate, endDate);
    }

    // Checks if a resource has conflicts during the requested dates, excluding assignments for a specific booking
    public boolean hasConflictExcludingBooking(Long resourceId, LocalDate startDate, LocalDate endDate, Long bookingId) {
        // Step 1: Return false if any required parameter is missing
        if (resourceId == null || startDate == null || endDate == null) {
            return false;
        }

        // Step 2: Fetch active assignments for the resource
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findActiveAssignmentsForResource(resourceId);
        if (assignments == null || assignments.isEmpty()) {
            List<ResourceAssignment> overlapping = resourceAssignmentRepository.findOverlapping(resourceId, startDate, endDate);
            if (overlapping != null && !overlapping.isEmpty()) {
                assignments = overlapping;
            } else if (assignments == null) {
                assignments = Collections.emptyList();
            }
        }

        // Step 3: Filter out assignments belonging to the specified booking
        if (bookingId != null && !assignments.isEmpty()) {
            assignments = assignments.stream()
                    .filter(a -> a.getBookingId() == null || !a.getBookingId().equals(bookingId))
                    .collect(Collectors.toList());
        }

        // Step 4: Evaluate availability via strategy
        return !strategy.isAvailable(assignments, startDate, endDate);
    }

    // Returns the list of overlapping assignments that conflict with the requested dates
    public List<ResourceAssignment> findConflictingAssignments(Long resourceId, LocalDate startDate, LocalDate endDate) {
        // Step 1: Return empty list if any required parameter is missing
        if (resourceId == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }

        // Step 2: Fetch and return all conflicting assignments
        return resourceAssignmentRepository.findOverlapping(resourceId, startDate, endDate);
    }

    // Returns the active availability strategy
    public ResourceAvailabilityStrategy getStrategy() {
        return strategy;
    }
}
