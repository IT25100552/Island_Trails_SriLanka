package com.islandtrails.payment.service;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.entity.BookingStatus;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.entity.*;
import com.islandtrails.payment.repository.PaymentRepository;
import com.islandtrails.payment.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         BookingService bookingService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    public List<Refund> getAllRefunds() {
        return refundRepository.findAllByOrderByRequestedAtDesc();
    }

    public List<Refund> getRefundsByCustomer(Long customerId) {
        return refundRepository.findByCustomerIdOrderByRequestedAtDesc(customerId);
    }

    public List<Refund> getPendingRefunds() {
        return refundRepository.findByStatusOrderByRequestedAtDesc(RefundStatus.REQUESTED);
    }

    public Refund getRefundById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found with id: " + id));
    }

    @Transactional
    public Refund requestRefund(Long bookingId, Long customerId, String customerName, String reason) {
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ValidationException("Booking is already cancelled.");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ValidationException("No verified payment found for this booking."));

        if (payment.getStatus() != PaymentStatus.VERIFIED) {
            throw new ValidationException("Cannot request refund for unverified payment.");
        }

        if (refundRepository.findByBookingId(bookingId).isPresent()) {
            throw new ValidationException("A refund has already been requested for this booking.");
        }

        Refund refund = new Refund(
                payment.getId(),
                booking.getId(),
                customerId,
                customerName,
                reason,
                payment.getAmount() // Full amount requested
        );

        return refundRepository.save(refund);
    }

    @Transactional
    public Refund approveRefund(Long refundId, Long financeOfficerId, String financeOfficerName, String decisionReason) {
        Refund refund = getRefundById(refundId);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ValidationException("Only REQUESTED refunds can be approved.");
        }

        // Refund policy: ALWAYS 100% refund
        refund.setApprovedAmount(refund.getRequestedAmount());
        refund.setStatus(RefundStatus.APPROVED);
        refund.setDecidedBy(financeOfficerId);
        refund.setDecidedByName(financeOfficerName);
        refund.setDecisionReason(decisionReason);
        refund.setRefundedAt(LocalDateTime.now());
        refund.setDecidedAt(LocalDateTime.now());
        refund.setRefundConfirmationCode("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Refund saved = refundRepository.save(refund);

        // Cancel the booking and release resources
        bookingService.cancelBooking(refund.getBookingId());

        return saved;
    }

    @Transactional
    public Refund rejectRefund(Long refundId, Long financeOfficerId, String financeOfficerName, String decisionReason) {
        Refund refund = getRefundById(refundId);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ValidationException("Only REQUESTED refunds can be rejected.");
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setDecidedBy(financeOfficerId);
        refund.setDecidedByName(financeOfficerName);
        refund.setDecisionReason(decisionReason);
        refund.setDecidedAt(LocalDateTime.now());

        return refundRepository.save(refund);
    }

    /**
     * Generates a financial reconciliation report summary.
     */
    public Map<String, Object> generateFinancialReport() {
        List<Payment> allPayments = paymentRepository.findAll();
        List<Refund> allRefunds = refundRepository.findAll();

        BigDecimal totalGrossRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        long verifiedCount = 0;
        long refundedCount = 0;
        long declinedCount = 0;
        long duplicateCount = 0;

        for (Payment p : allPayments) {
            if (p.getStatus() == PaymentStatus.VERIFIED) {
                totalGrossRevenue = totalGrossRevenue.add(p.getAmount());
                verifiedCount++;
            } else if (p.getStatus() == PaymentStatus.DECLINED) {
                declinedCount++;
            } else if (p.getStatus() == PaymentStatus.DUPLICATE_FLAGGED) {
                duplicateCount++;
            }
        }

        for (Refund r : allRefunds) {
            if (r.getStatus() == RefundStatus.APPROVED && r.getApprovedAmount() != null) {
                totalRefunded = totalRefunded.add(r.getApprovedAmount());
                refundedCount++;
            }
        }

        BigDecimal netRevenue = totalGrossRevenue.subtract(totalRefunded);

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

        return report;
    }
}
