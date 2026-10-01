package com.islandtrails.payment.repository;

import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    List<Payment> findAllByOrderByCreatedAtDesc();
}
