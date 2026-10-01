package com.islandtrails.payment.repository;

import com.islandtrails.payment.entity.Refund;
import com.islandtrails.payment.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByCustomerIdOrderByRequestedAtDesc(Long customerId);
    List<Refund> findByStatusOrderByRequestedAtDesc(RefundStatus status);
    Optional<Refund> findByBookingId(Long bookingId);
    Optional<Refund> findByPaymentId(Long paymentId);
    List<Refund> findAllByOrderByRequestedAtDesc();
}
