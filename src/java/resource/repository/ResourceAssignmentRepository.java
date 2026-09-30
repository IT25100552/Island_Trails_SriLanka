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

    @Query("SELECT ra FROM ResourceAssignment ra WHERE ra.resourceId = :resourceId " +
           "AND ra.startDate <= :endDate AND ra.endDate >= :startDate " +
           "AND ra.status IN ('RESERVED', 'CONFIRMED')")
    List<ResourceAssignment> findOverlapping(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<ResourceAssignment> findByPackageId(Long packageId);

    List<ResourceAssignment> findByBookingId(Long bookingId);

    List<ResourceAssignment> findByResourceId(Long resourceId);

    @Query("SELECT ra FROM ResourceAssignment ra WHERE ra.startDate <= :endDate AND ra.endDate >= :startDate")
    List<ResourceAssignment> findAllInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
