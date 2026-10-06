package com.islandtrails.resource.strategy;

import com.islandtrails.common.util.ConflictCheckUtil;
import com.islandtrails.resource.entity.ResourceAssignment;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component("strictAvailabilityStrategy")
@Primary
public class StrictAvailabilityStrategy implements ResourceAvailabilityStrategy {

    @Override
    public boolean isAvailable(List<ResourceAssignment> existingAssignments, LocalDate startDate, LocalDate endDate) {
        if (existingAssignments == null || existingAssignments.isEmpty()) {
            return true;
        }

        if (startDate == null || endDate == null) {
            return false;
        }

        for (ResourceAssignment a : existingAssignments) {
            if (a == null || a.getStartDate() == null || a.getEndDate() == null) {
                continue;
            }

            if (ConflictCheckUtil.hasDateOverlap(a.getStartDate(), a.getEndDate(), startDate, endDate)) {
                return false;
            }
        }

        return true;
    }
}
