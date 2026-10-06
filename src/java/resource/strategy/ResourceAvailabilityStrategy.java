package com.islandtrails.resource.strategy;

import com.islandtrails.resource.entity.ResourceAssignment;

import java.time.LocalDate;
import java.util.List;


public interface ResourceAvailabilityStrategy {

    /**
     * Determines whether a resource is available for the requested date window given existing assignments.
     *
     * @param existingAssignments existing assignments to check against
     * @param startDate           requested start date
     * @param endDate             requested end date
     * @return true if available (no conflict), false if unavailable (conflict)
     */
    boolean isAvailable(List<ResourceAssignment> existingAssignments, LocalDate startDate, LocalDate endDate);
}
