package com.islandtrails.resource.strategy;

import com.islandtrails.common.util.ConflictCheckUtil;
import com.islandtrails.resource.entity.ResourceAssignment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Buffered resource availability strategy that adds a buffer window (turnaround time)
 * before and after existing assignments to prevent tight turnaround collisions.
 */
@Component("bufferAvailabilityStrategy")
public class BufferAvailabilityStrategy implements ResourceAvailabilityStrategy {

    private final int bufferDays;

    public BufferAvailabilityStrategy() {
        this(1);
    }

    public BufferAvailabilityStrategy(int bufferDays) {
        this.bufferDays = Math.max(0, bufferDays);
    }

    public int getBufferDays() {
        return bufferDays;
    }

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

            LocalDate bufferedStart = a.getStartDate().minusDays(bufferDays);
            LocalDate bufferedEnd = a.getEndDate().plusDays(bufferDays);

            if (ConflictCheckUtil.hasDateOverlap(bufferedStart, bufferedEnd, startDate, endDate)) {
                return false;
            }
        }

        return true;
    }
}
