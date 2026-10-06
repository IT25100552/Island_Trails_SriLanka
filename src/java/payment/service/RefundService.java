package com.islandtrails.payment.service;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.UnauthorizedException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.entity.*;
import com.islandtrails.payment.repository.PaymentRepository;
import com.islandtrails.payment.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

// Service managing customer refund requests, approvals, rejections, and financial reporting
@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    // Injects required repositories and booking service
    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         BookingService bookingService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    // Returns all refund requests ordered by newest first
    public List<Refund> getAllRefunds() {
        return refundRepository.findAllByOrderByRequestedAtDesc();
    }

    // Returns all refund requests for a specific customer
    public List<Refund> getRefundsByCustomer(Long customerId) {
        return refundRepository.findByCustomerIdOrderByRequestedAtDesc(customerId);
    }

    // Returns all refund requests that are currently waiting for review (REQUESTED status)
    public List<Refund> getPendingRefunds() {
        return refundRepository.findByStatusOrderByRequestedAtDesc(RefundStatus.REQUESTED);
    }

    // Finds a refund request by ID or throws an error if not found
    public Refund getRefundById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found with id: " + id));
    }

    // Submits a 100% refund request for a verified booking
    @Transactional
    public Refund requestRefund(Long bookingId, Long customerId, String customerName, String reason) {
        // Step 1: Check that the booking is not already cancelled
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ValidationException("Booking is already cancelled.");
        }

        // Step 1a: Enforce that the customer owns this booking
        if (!booking.getCustomerId().equals(customerId)) {
            throw new ValidationException("You are not authorized to request a refund for this booking.");
        }

        // Step 2: Ensure that a verified payment exists for this booking
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ValidationException("No verified payment found for this booking."));

        if (payment.getStatus() != PaymentStatus.VERIFIED) {
            throw new ValidationException("Cannot request refund for unverified payment.");
        }

        // Step 3: Block only if a refund is pending review or already approved.
        // A previously REJECTED refund does not block a new submission (treated as an appeal).
        List<Refund> existingRefunds = refundRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        boolean activeRefundExists = existingRefunds.stream()
                .anyMatch(r -> r.getStatus() == RefundStatus.REQUESTED || r.getStatus() == RefundStatus.APPROVED);
        if (activeRefundExists) {
            throw new ValidationException("A refund for this booking is already pending review or has been approved.");
        }

        // Step 4: Create the refund entity requesting 100% of the paid amount
        Refund refund = new Refund(
                payment.getId(),
                booking.getId(),
                customerId,
                customerName,
                reason,
                payment.getAmount() // Full amount requested
        );

        // Step 4a: Mark the payment as REFUND_REQUESTED
        payment.setStatus(PaymentStatus.REFUND_REQUESTED);
        paymentRepository.save(payment);

        // Step 5: Save and return the refund request in REQUESTED status
        return refundRepository.save(refund);
    }

    // Lets a customer withdraw their own pending refund request (hard delete, booking stays confirmed)
    @Transactional
    public void withdrawRefundRequest(Long refundId, Long customerId) {
        // Step 1: Look up the refund
        Refund refund = getRefundById(refundId);

        // Step 2: Ownership check (IDOR guard)
        if (refund.getCustomerId() == null || !refund.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("You are not authorized to withdraw this refund request.");
        }

        // Step 3: Only requests still waiting for review can be withdrawn
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ValidationException("Only refund requests pending review can be withdrawn.");
        }

        // Step 4: Make sure the linked payment is back to VERIFIED (never touch a REFUNDED payment).
        // Note: requestRefund() does not move the payment out of VERIFIED, so this is normally a no-op.
        if (refund.getPaymentId() != null) {
            paymentRepository.findById(refund.getPaymentId()).ifPresent(payment -> {
                if (payment.getStatus() != PaymentStatus.VERIFIED && payment.getStatus() != PaymentStatus.REFUNDED) {
                    payment.setStatus(PaymentStatus.VERIFIED);
                    paymentRepository.save(payment);
                }
            });
        }



        // Step 5: Hard-delete the refund row
        refundRepository.delete(refund);
    }

    // Approves a 100% refund and cancels booking while preserving payment and booking records for audit history
    @Transactional
    public Refund approveRefund(Long refundId, Long financeOfficerId, String financeOfficerName, String decisionReason) {
        // Step 1: Look up refund and verify it is currently in REQUESTED status
        Refund refund = getRefundById(refundId);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ValidationException("Only REQUESTED refunds can be approved.");
        }

        // Step 2: Set 100% approved amount, status to APPROVED, and generate confirmation code
        refund.setApprovedAmount(refund.getRequestedAmount());
        refund.setStatus(RefundStatus.APPROVED);
        refund.setDecidedBy(financeOfficerId);
        refund.setDecidedByName(financeOfficerName);
        refund.setDecisionReason(decisionReason);
        refund.setRefundedAt(LocalDateTime.now());
        refund.setDecidedAt(LocalDateTime.now());
        refund.setRefundConfirmationCode("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // Step 3: Save the approved refund record
        Refund saved = refundRepository.save(refund);

        // Step 4: Update linked Payment status to REFUNDED
        if (refund.getPaymentId() != null) {
            paymentRepository.findById(refund.getPaymentId()).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            });
        }

        // Step 5: Cancel booking to release reserved resources, update status, and archive the refunded booking
        if (refund.getBookingId() != null) {
            bookingService.cancelBooking(refund.getBookingId());
            bookingService.archiveBooking(refund.getBookingId());
        }

        // Step 6: Return the approved refund
        return saved;
    }

    // Rejects a refund request with a given reason
    @Transactional
    public Refund rejectRefund(Long refundId, Long financeOfficerId, String financeOfficerName, String decisionReason) {
        // Step 1: Look up refund and verify it is currently in REQUESTED status
        Refund refund = getRefundById(refundId);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ValidationException("Only REQUESTED refunds can be rejected.");
        }

        // Step 2: Update status to REJECTED and record decision details
        refund.setStatus(RefundStatus.REJECTED);
        refund.setDecidedBy(financeOfficerId);
        refund.setDecidedByName(financeOfficerName);
        refund.setDecisionReason(decisionReason);
        refund.setDecidedAt(LocalDateTime.now());

        // Step 2a: Revert linked payment status from REFUND_REQUESTED back to VERIFIED to allow appeal
        if (refund.getPaymentId() != null) {
            paymentRepository.findById(refund.getPaymentId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.REFUND_REQUESTED) {
                    payment.setStatus(PaymentStatus.VERIFIED);
                    paymentRepository.save(payment);
                }
            });
        }

        // Step 3: Save and return the updated refund
        return refundRepository.save(refund);
    }

    // Generates a summary report of revenue, refunds, and transaction statistics
    public Map<String, Object> generateFinancialReport() {
        // Step 1: Fetch all payments and refunds from the database
        List<Payment> allPayments = paymentRepository.findAll();
        List<Refund> allRefunds = refundRepository.findAll();

        // Step 2: Initialize totals and counters
        BigDecimal totalGrossRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        long verifiedCount = 0;
        long refundedCount = 0;
        long declinedCount = 0;
        long duplicateCount = 0;

        // Step 3: Calculate gross revenue and payment counts by status (VERIFIED + REFUNDED)
        for (Payment p : allPayments) {
            if (p.getStatus() == PaymentStatus.VERIFIED || p.getStatus() == PaymentStatus.REFUNDED) {
                totalGrossRevenue = totalGrossRevenue.add(p.getAmount());
                if (p.getStatus() == PaymentStatus.VERIFIED) {
                    verifiedCount++;
                }
            } else if (p.getStatus() == PaymentStatus.DECLINED) {
                declinedCount++;
            } else if (p.getStatus() == PaymentStatus.DUPLICATE_FLAGGED) {
                duplicateCount++;
            }
        }

        // Step 4: Calculate total refunded amount from approved refunds
        for (Refund r : allRefunds) {
            if (r.getStatus() == RefundStatus.APPROVED && r.getApprovedAmount() != null) {
                totalRefunded = totalRefunded.add(r.getApprovedAmount());
                refundedCount++;
            }
        }

        // Step 5: Compute net revenue (gross revenue minus refunds)
        BigDecimal netRevenue = totalGrossRevenue.subtract(totalRefunded);

        // Step 6: Build the report map with all metrics and recent lists
        Map<String, Object> report = new HashMap<>();
        report.put("totalGrossRevenue", totalGrossRevenue);
        report.put("totalRefunded", totalRefunded);
        report.put("netRevenue", netRevenue);
        report.put("verifiedPaymentsCount", verifiedCount);
        report.put("approvedRefundsCount", refundedCount);
        report.put("declinedPaymentsCount", declinedCount);
        report.put("duplicatePaymentsCount", duplicateCount);
        report.put("recentPayments", paymentRepository.findAllByOrderByCreatedAtDesc());
        report.put("recentRefunds", refundRepository.findAllByOrderByRequestedAtDesc());

        // Step 7: Return the completed report
        return report;
    }
}
