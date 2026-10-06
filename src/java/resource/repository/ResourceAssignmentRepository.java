package com.islandtrails.resource.repository;

import com.islandtrails.resource.entity.ResourceAssignment;
import com.islandtrails.resource.entity.ResourceAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ResourceAssignmentRepository extends JpaRepository<ResourceAssignment, Long> {

    // Checks if a resource has an overlapping booking between start and end date
    @Query("SELECT ra FROM ResourceAssignment ra WHERE ra.resourceId = :resourceId " +
           "AND ra.bookingId IS NOT NULL " +
           "AND ra.startDate <= :endDate AND ra.endDate >= :startDate " +
           "AND ra.status IN ('RESERVED', 'CONFIRMED')")
    List<ResourceAssignment> findOverlapping(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Finds all template resource assignments configured for a tour package (excluding booking instances)
    List<ResourceAssignment> findByPackageIdAndBookingIdIsNull(Long packageId);

    // Finds all resource assignments configured for a tour package
    List<ResourceAssignment> findByPackageId(Long packageId);

    // Finds all resource assignments linked to a specific booking
    List<ResourceAssignment> findByBookingId(Long bookingId);

    // Finds all assignments for a single resource across all bookings
    List<ResourceAssignment> findByResourceId(Long resourceId);

    // Finds all active assignments (reserved or confirmed) for a specific resource
    @Query("SELECT ra FROM ResourceAssignment ra WHERE ra.resourceId = :resourceId " +
           "AND ra.status IN ('RESERVED', 'CONFIRMED')")
    List<ResourceAssignment> findActiveAssignmentsForResource(@Param("resourceId") Long resourceId);

    // Checks if any assignment exists for a given resource
    boolean existsByResourceId(Long resourceId);

    // Counts how many distinct packages a resource is assigned to
    @Query("SELECT COUNT(DISTINCT ra.packageId) FROM ResourceAssignment ra WHERE ra.resourceId = :resourceId AND ra.packageId IS NOT NULL")
    long countDistinctPackagesByResourceId(@Param("resourceId") Long resourceId);

    // Finds all assignments that touch the given date range regardless of status
    @Query("SELECT ra FROM ResourceAssignment ra WHERE ra.startDate <= :endDate AND ra.endDate >= :startDate")
    List<ResourceAssignment> findAllInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Deletes all assignments associated with a package template
    void deleteByPackageId(Long packageId);

    // Deletes all assignments associated with a specific resource
    void deleteByResourceId(Long resourceId);
}
