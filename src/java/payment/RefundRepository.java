package com.islandtrails.payment.repository;

import com.islandtrails.payment.entity.Refund;
import com.islandtrails.payment.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repository for managing refund database records
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    // Get all refund requests for a specific customer (newest first)
    List<Refund> findByCustomerIdOrderByRequestedAtDesc(Long customerId);

    // Checks if any refund request exists for a specific customer
    boolean existsByCustomerId(Long customerId);

    // Get all refund requests filtered by status (newest first)
    List<Refund> findByStatusOrderByRequestedAtDesc(RefundStatus status);

    // Get every refund cycle (original request + appeals) for a booking, newest first
    List<Refund> findByBookingIdOrderByRequestedAtDesc(Long bookingId);

    // Find refund request by payment ID
    Optional<Refund> findByPaymentId(Long paymentId);

    // Get all refund requests ordered by request date (newest first)
    List<Refund> findAllByOrderByRequestedAtDesc();

    // Checks if any refund decision was made by a specific finance officer
    boolean existsByDecidedBy(Long decidedBy);
}
