package com.islandtrails.resource.service;

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

    public AvailabilityService(ResourceRepository resourceRepository,
                               ResourceAssignmentRepository resourceAssignmentRepository,
                               ConflictDetectionService conflictDetectionService) {
        this.resourceRepository = resourceRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.conflictDetectionService = conflictDetectionService;
    }

    public List<ResourceAssignment> getAssignmentsInDateRange(LocalDate startDate, LocalDate endDate) {
        return resourceAssignmentRepository.findAllInDateRange(startDate, endDate);
    }

    public List<Resource> getAvailableResources(LocalDate startDate, LocalDate endDate) {
        List<Resource> activeResources = resourceRepository.findByStatus(ResourceStatus.ACTIVE);
        List<Resource> available = new ArrayList<>();

        for (Resource resource : activeResources) {
            if (!conflictDetectionService.hasConflict(resource.getId(), startDate, endDate)) {
                available.add(resource);
            }
        }
        return available;
    }

    @Transactional
    public ResourceAssignment reserveResource(Long resourceId, Long packageId, Long bookingId, LocalDate startDate, LocalDate endDate) {
        ResourceAssignment assignment = new ResourceAssignment(packageId, resourceId, startDate, endDate, ResourceAssignmentStatus.RESERVED);
        assignment.setBookingId(bookingId);
        return resourceAssignmentRepository.save(assignment);
    }

    @Transactional
    public void confirmAssignmentsForBooking(Long bookingId) {
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findByBookingId(bookingId);
        for (ResourceAssignment ra : assignments) {
            ra.setStatus(ResourceAssignmentStatus.CONFIRMED);
            resourceAssignmentRepository.save(ra);
        }
    }

    @Transactional
    public void releaseAssignmentsForBooking(Long bookingId) {
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findByBookingId(bookingId);
        for (ResourceAssignment ra : assignments) {
            ra.setStatus(ResourceAssignmentStatus.AVAILABLE);
            resourceAssignmentRepository.delete(ra);
        }
    }

    /**
     * Calculates resource utilization report over a given date range.
     */
    public Map<String, Object> calculateUtilizationReport(LocalDate startDate, LocalDate endDate) {
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays <= 0) totalDays = 1;

        List<Resource> resources = resourceRepository.findAll();
        List<ResourceAssignment> assignments = resourceAssignmentRepository.findAllInDateRange(startDate, endDate);

        Map<Long, Long> bookedDaysPerResource = new HashMap<>();
        for (ResourceAssignment ra : assignments) {
            if (ra.getStatus() == ResourceAssignmentStatus.RESERVED || ra.getStatus() == ResourceAssignmentStatus.CONFIRMED) {
                LocalDate effectiveStart = ra.getStartDate().isBefore(startDate) ? startDate : ra.getStartDate();
                LocalDate effectiveEnd = ra.getEndDate().isAfter(endDate) ? endDate : ra.getEndDate();
                long days = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
                bookedDaysPerResource.merge(ra.getResourceId(), Math.max(0, days), Long::sum);
            }
        }

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

        double averageUtilization = resources.isEmpty() ? 0.0 : totalUtilizationSum / resources.size();

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
