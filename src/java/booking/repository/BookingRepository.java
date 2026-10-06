package com.islandtrails.booking.repository;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Finds all bookings placed by a specific customer, newest first
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Finds all non-archived bookings placed by a specific customer, newest first
    List<Booking> findByCustomerIdAndArchivedFalseOrderByCreatedAtDesc(Long customerId);

    // Checks if any booking exists for a specific customer
    boolean existsByCustomerId(Long customerId);

    // Finds all bookings with a given status, newest first
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    // Gets all bookings across the system, newest first
    List<Booking> findAllByOrderByCreatedAtDesc();

    // Counts how many bookings currently have a given status
    long countByStatus(BookingStatus status);

    // Counts how many bookings belong to a specific package
    long countByPackageId(Long packageId);

    // Counts how many bookings belong to a specific package with specified statuses
    long countByPackageIdAndStatusIn(Long packageId, java.util.Collection<BookingStatus> statuses);
}
