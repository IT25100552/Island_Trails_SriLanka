package com.islandtrails.payment.service;

import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.DuplicatePaymentException;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.PaymentMethod;
import com.islandtrails.payment.entity.PaymentStatus;
import com.islandtrails.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final PaymentGatewayService paymentGatewayService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          InvoiceService invoiceService,
                          PaymentGatewayService paymentGatewayService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.paymentGatewayService = paymentGatewayService;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Payment> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Optional<Payment> getPaymentByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Transactional
    public Payment processPayment(Payment payment) throws DuplicatePaymentException {
        // Check for duplicate payment: same transactionId, amount, and bookingId
        if (payment.getTransactionId() != null) {
            Optional<Payment> existing = paymentRepository.findByTransactionId(payment.getTransactionId());
            if (existing.isPresent()) {
                Payment dup = existing.get();
                if (dup.getAmount().compareTo(payment.getAmount()) == 0 &&
                    dup.getBookingId().equals(payment.getBookingId()) &&
                    dup.getStatus() == PaymentStatus.VERIFIED) {

                    payment.setStatus(PaymentStatus.DUPLICATE_FLAGGED);
                    paymentRepository.save(payment);
                    throw new DuplicatePaymentException("Duplicate transaction detected for transaction ID: " + payment.getTransactionId());
                }
            }
        }

        // Verify payment
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setProcessedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        // Update booking status to CONFIRMED
        bookingService.confirmBooking(payment.getBookingId());

        // Automatically generate invoice
        invoiceService.generateInvoice(payment.getBookingId());

        return saved;
    }

    @Transactional
    public Payment makePayment(Long bookingId, Long customerId, BigDecimal amount, PaymentMethod method, String cardNumber) throws DuplicatePaymentException {
        String transactionId = paymentGatewayService.generateTransactionId();
        boolean success = paymentGatewayService.simulatePayment(cardNumber);

        Payment payment = new Payment(bookingId, customerId, amount, method, transactionId);

        if (!success) {
            payment.setStatus(PaymentStatus.DECLINED);
            return paymentRepository.save(payment);
        }

        return processPayment(payment);
    }
}
