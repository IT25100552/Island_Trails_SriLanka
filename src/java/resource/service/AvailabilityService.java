package com.islandtrails.resource.service;

import com.islandtrails.common.exception.ResourceConflictException;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceAssignment;
import com.islandtrails.resource.entity.ResourceAssignmentStatus;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import com.islandtrails.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AvailabilityService {

    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final ConflictDetectionService conflictDetectionService;

    // Injects repositories and conflict detection service
    public AvailabilityService(ResourceRepository resourceRepository,
                               ResourceAssignmentRepository resourceAssignmentRepository,
                               ConflictDetectionService conflictDetectionService) {
        this.resourceRepository = resourceRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.conflictDetectionService = conflictDetectionService;
    }

    // Gets all resource assignments that fall within a date range
    public List<ResourceAssignment> getAssignmentsInDateRange(LocalDate startDate, LocalDate endDate) {
        return resourceAssignmentRepository.findAllInDateRange(startDate, endDate);
    }

    // Finds all active resources that have no booking conflicts during the dates
    public List<Resource> getAvailableResources(LocalDate startDate, LocalDate endDate) {
        // Step 1: Fetch all active resources
        List<Resource> activeResources = resourceRepository.findByStatus(ResourceStatus.ACTIVE);
        List<Resource> available = new ArrayList<>();

        // Step 2: Filter for resources with no overlapping bookings
        for (Resource resource : activeResources) {
            if (!conflictDetectionService.hasConflict(resource.getId(), startDate, endDate)) {
                available.add(resource);
            }
        }

        // Step 3: Return the available resources
        return available;
    }

    // Tentatively holds a resource for a booking with RESERVED status, atomically checking for conflicts
    @Transactional
    public synchronized ResourceAssignment reserveResource(Long resourceId, Long packageId, Long bookingId, LocalDate startDate, LocalDate endDate) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + resourceId));
        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new ValidationException("Cannot assign inactive resource: '" + resource.getName() + "'. Please activate it first.");
        }

        // Step 1: Atomic check to prevent check-then-act race conditions
        if (conflictDetectionService.hasConflict(resourceId, startDate, endDate)) {
            throw new ResourceConflictException("Resource conflict detected for selected travel dates (" + startDate + " to " + endDate + ").");
        }

        // Step 2: Create a new assignment in RESERVED status
        ResourceAssignment assignment = new ResourceAssignment(packageId, resourceId, startDate, endDate, ResourceAssignmentStatus.RESERVED);

        // Step 3: Link the assignment to the booking ID
        assignment.setBookingId(bookingId);

        // Step 4: Save and return the assignment
        return resourceAssignmentRepository.save(assignment);
    }

    // Upgrades all reserved resources for a booking to CONFIRMED status
    @Transactional
    public void confirmAssignmentsForBooking(Long bookingId) {
        // Step 1: Find all assignments for this booking
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findByBookingId(bookingId);

        // Step 2: Set status to CONFIRMED and save each one
        for (ResourceAssignment ra : assignments) {
            ra.setStatus(ResourceAssignmentStatus.CONFIRMED);
            resourceAssignmentRepository.save(ra);
        }
    }

    // Releases and removes all resource holds for a booking
    @Transactional
    public void releaseAssignmentsForBooking(Long bookingId) {
        // Step 1: Find all assignments for this booking
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findByBookingId(bookingId);

        // Step 2: Mark as AVAILABLE and delete the assignment record
        for (ResourceAssignment ra : assignments) {
            ra.setStatus(ResourceAssignmentStatus.AVAILABLE);
            resourceAssignmentRepository.delete(ra);
        }
    }

    // Computes resource utilization percentage and stats over a date window
    public Map<String, Object> calculateUtilizationReport(LocalDate startDate, LocalDate endDate) {
        // Step 1: Calculate total days in the date range
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays <= 0) totalDays = 1;

        // Step 2: Load all resources and assignments in this date range
        List<Resource> resources = resourceRepository.findAll();
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findAllInDateRange(startDate, endDate);

        // Step 3: Count booked days per resource within the date window
        Map<Long, Long> bookedDaysPerResource = new HashMap<>();
        for (ResourceAssignment ra : assignments) {
            if (ra.getStatus() == ResourceAssignmentStatus.RESERVED || ra.getStatus() == ResourceAssignmentStatus.CONFIRMED) {
                LocalDate effectiveStart = ra.getStartDate().isBefore(startDate) ? startDate : ra.getStartDate();
                LocalDate effectiveEnd = ra.getEndDate().isAfter(endDate) ? endDate : ra.getEndDate();
                long days = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
                bookedDaysPerResource.merge(ra.getResourceId(), Math.max(0, days), Long::sum);
            }
        }

        // Step 4: Calculate utilization percentage for each resource
        List<Map<String, Object>> resourceStats = new ArrayList<>();
        double totalUtilizationSum = 0;

        for (Resource r : resources) {
            long bookedDays = bookedDaysPerResource.getOrDefault(r.getId(), 0L);
            double utilizationPercent = Math.min(100.0, (double) bookedDays / totalDays * 100.0);
            totalUtilizationSum += utilizationPercent;

            Map<String, Object> stat = new HashMap<>();
            stat.put("resourceId", r.getId());
            stat.put("resourceName", r.getName());
            stat.put("resourceType", r.getResourceType().name());
            stat.put("bookedDays", bookedDays);
            stat.put("totalDays", totalDays);
            stat.put("utilizationPercentage", Math.round(utilizationPercent * 10.0) / 10.0);
            resourceStats.add(stat);
        }

        // Step 5: Calculate overall average utilization
        double averageUtilization = resources.isEmpty() ? 0.0 : totalUtilizationSum / resources.size();

        // Step 6: Build and return the report map
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalDays", totalDays);
        report.put("totalResources", resources.size());
        report.put("averageUtilization", Math.round(averageUtilization * 10.0) / 10.0);
        report.put("resourceStats", resourceStats);

        return report;
    }
}
