package com.islandtrails.payment.repository;

import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repository for managing payment database records
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment record by booking ID
    Optional<Payment> findByBookingId(Long bookingId);

    // Find payment record by transaction reference ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Get all payments for a specific customer (newest first)
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Checks if any payment exists for a specific customer
    boolean existsByCustomerId(Long customerId);

    // Get all payments filtered by status (newest first)
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    // Get payments matching any of the specified statuses ordered by processed time (newest first)
    List<Payment> findByStatusInOrderByProcessedAtDesc(List<PaymentStatus> statuses);

    // Get payments matching status ordered by processed time (newest first)
    List<Payment> findByStatusOrderByProcessedAtDesc(PaymentStatus status);

    // Get all payments ordered by creation date (newest first)
    List<Payment> findAllByOrderByCreatedAtDesc();
}
