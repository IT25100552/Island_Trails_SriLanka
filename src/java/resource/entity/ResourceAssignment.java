package com.islandtrails.resource.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "resource_assignments")
public class ResourceAssignment extends BaseEntity {

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceAssignmentStatus status = ResourceAssignmentStatus.AVAILABLE;

    public ResourceAssignment() {
    }

    public ResourceAssignment(Long packageId, Long resourceId, LocalDate startDate, LocalDate endDate, ResourceAssignmentStatus status) {
        this.packageId = packageId;
        this.resourceId = resourceId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ResourceAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceAssignmentStatus status) {
        this.status = status;
    }
}
