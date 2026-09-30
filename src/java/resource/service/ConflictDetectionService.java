package com.islandtrails.resource.service;

import com.islandtrails.resource.entity.ResourceAssignment;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class ConflictDetectionService {

    private final ResourceAssignmentRepository resourceAssignmentRepository;

    public ConflictDetectionService(ResourceAssignmentRepository resourceAssignmentRepository) {
        this.resourceAssignmentRepository = resourceAssignmentRepository;
    }

    /**
     * Checks if a resource is already reserved or confirmed for an overlapping date range.
     * JPA query condition: ra.startDate <= endDate AND ra.endDate >= startDate
     */
    public boolean hasConflict(Long resourceId, LocalDate startDate, LocalDate endDate) {
        if (resourceId == null || startDate == null || endDate == null) {
            return false;
        }
        List<ResourceAssignment> overlapping = resourceAssignmentRepository.findOverlapping(resourceId, startDate, endDate);
        return !overlapping.isEmpty();
    }

    public List<ResourceAssignment> findConflictingAssignments(Long resourceId, LocalDate startDate, LocalDate endDate) {
        if (resourceId == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }
        return resourceAssignmentRepository.findOverlapping(resourceId, startDate, endDate);
    }
}
