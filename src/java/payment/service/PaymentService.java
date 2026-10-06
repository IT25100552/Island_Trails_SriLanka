package com.islandtrails.payment.service;

import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.DuplicatePaymentException;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.PaymentMethod;
import com.islandtrails.payment.entity.PaymentStatus;
import com.islandtrails.payment.repository.PaymentRepository;
import com.islandtrails.payment.strategy.PaymentProcessingStrategy;
import com.islandtrails.payment.strategy.PaymentProcessingStrategyFactory;
import com.islandtrails.tripplanning.entity.TripRequestStatus;
import com.islandtrails.tripplanning.repository.TripRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Service handling payment processing, verification, and payment record management
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final PaymentGatewayService paymentGatewayService;
    private final TripRequestRepository tripRequestRepository;
    private final BookingRepository bookingRepository;
    private final PaymentProcessingStrategyFactory strategyFactory;
    private final com.islandtrails.payment.repository.RefundRepository refundRepository;

    // Injects required repositories, services, and strategy factory
    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          InvoiceService invoiceService,
                          PaymentGatewayService paymentGatewayService,
                          TripRequestRepository tripRequestRepository,
                          BookingRepository bookingRepository,
                          PaymentProcessingStrategyFactory strategyFactory,
                          com.islandtrails.payment.repository.RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.paymentGatewayService = paymentGatewayService;
        this.tripRequestRepository = tripRequestRepository;
        this.bookingRepository = bookingRepository;
        this.strategyFactory = strategyFactory != null ? strategyFactory : new PaymentProcessingStrategyFactory(paymentGatewayService);
        this.refundRepository = refundRepository;
    }

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          InvoiceService invoiceService,
                          PaymentGatewayService paymentGatewayService,
                          TripRequestRepository tripRequestRepository,
                          BookingRepository bookingRepository,
                          PaymentProcessingStrategyFactory strategyFactory) {
        this(paymentRepository, bookingService, invoiceService, paymentGatewayService, tripRequestRepository, bookingRepository, strategyFactory, null);
    }

    // Test constructor for backwards compatibility
    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          InvoiceService invoiceService,
                          PaymentGatewayService paymentGatewayService,
                          TripRequestRepository tripRequestRepository,
                          BookingRepository bookingRepository) {
        this(paymentRepository, bookingService, invoiceService, paymentGatewayService, tripRequestRepository, bookingRepository,
                new PaymentProcessingStrategyFactory(paymentGatewayService), null);
    }

    // Returns all payment records ordered by newest first
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    // Returns all payments made by a specific customer
    public List<Payment> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    // Finds the payment record for a given booking ID
    public Optional<Payment> getPaymentByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    // Finds a payment by its ID or throws an error if not found
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    // Returns all active payments (PENDING, VERIFIED or REFUND_REQUESTED status) ordered by processed date descending
    public List<Payment> getActivePayments() {
        return paymentRepository.findByStatusInOrderByProcessedAtDesc(
                List.of(PaymentStatus.PENDING, PaymentStatus.VERIFIED, PaymentStatus.REFUND_REQUESTED));
    }

    // Returns all refunded payments ordered by processed date descending
    public List<Payment> getRefundedPayments() {
        return paymentRepository.findByStatusOrderByProcessedAtDesc(PaymentStatus.REFUNDED);
    }

    // Returns all failed, declined, or flagged payments ordered by processed date descending
    public List<Payment> getFailedPayments() {
        return paymentRepository.findByStatusInOrderByProcessedAtDesc(
                List.of(PaymentStatus.DECLINED, PaymentStatus.TIMEOUT, PaymentStatus.DUPLICATE_FLAGGED));
    }

    // Verifies a payment, checks for duplicates, confirms the booking, and generates an invoice
    @Transactional
    public Payment processPayment(Payment payment) throws DuplicatePaymentException {
        // Step 1: Check if an identical verified payment already exists to prevent duplicate charges
        if (payment.getTransactionId() != null) {
            Optional<Payment> existing = paymentRepository.findByTransactionId(payment.getTransactionId());
            if (existing.isPresent()) {
                Payment dup = existing.get();
                if (dup.getAmount().compareTo(payment.getAmount()) == 0 &&
                    dup.getBookingId().equals(payment.getBookingId()) &&
                    dup.getStatus() == PaymentStatus.VERIFIED) {

                    // Step 1a: Flag this transaction as duplicate and stop processing
                    payment.setStatus(PaymentStatus.DUPLICATE_FLAGGED);
                    paymentRepository.save(payment);
                    throw new DuplicatePaymentException("Duplicate transaction detected for transaction ID: " + payment.getTransactionId());
                }
            }
        }

        // Step 2: Mark payment as VERIFIED and record the time
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setProcessedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        // Step 3: Confirm the customer's booking
        bookingService.confirmBooking(payment.getBookingId());

        // Step 4: Automatically generate an invoice for the booking
        invoiceService.generateInvoice(payment.getBookingId());

        // Step 5: Transition linked custom trip request to CONVERTED_TO_BOOKING if present
        if (tripRequestRepository != null) {
            tripRequestRepository.findByBookingId(payment.getBookingId()).ifPresent(req -> {
                req.setStatus(TripRequestStatus.CONVERTED_TO_BOOKING);
                tripRequestRepository.save(req);
            });
        }

        // Step 6: Return the saved payment record
        return saved;
    }

    // Runs an online card payment through the gateway simulation and confirms the booking on success
    @Transactional
    public Payment makePayment(Long bookingId, Long customerId, BigDecimal amount, PaymentMethod method, String cardNumber) throws DuplicatePaymentException {
        // Step 0: Verify booking eligibility and ownership
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new ValidationException("Access denied: You do not own this booking.");
        }

        if (booking.getStatus() != com.islandtrails.booking.entity.BookingStatus.PENDING_PAYMENT) {
            throw new ValidationException("Payment can only be processed for bookings in PENDING_PAYMENT status.");
        }

        // Step 1: Generate a transaction ID
        String transactionId = paymentGatewayService.generateTransactionId();

        // Step 2: Look up existing payment for this booking or create a new one
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElse(new Payment(bookingId, customerId, amount, method, transactionId));

        // Step 3: Prevent re-paying if the payment is already verified
        if (payment.getStatus() == PaymentStatus.VERIFIED || payment.getStatus() == PaymentStatus.REFUND_REQUESTED) {
            throw new ValidationException("Payment for this booking has already been accepted and cannot be modified.");
        }

        // Step 4: Fill in payment details and transaction ID (maintain original booking owner customerId)
        payment.setCustomerId(booking.getCustomerId());
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setTransactionId(transactionId);

        // Step 5: Delegate payment authorization to strategy
        PaymentProcessingStrategy strategy = strategyFactory.getStrategy(method);
        PaymentStatus outcome = strategy.process(amount, cardNumber);
        payment.setStatus(outcome);

        if (outcome == PaymentStatus.DECLINED) {
            return paymentRepository.save(payment);
        }

        if (outcome == PaymentStatus.VERIFIED) {
            return processPayment(payment);
        }

        // Step 6: For pending or other outcomes, save and return
        return paymentRepository.save(payment);
    }

    // Returns the payment processing strategy factory
    public PaymentProcessingStrategyFactory getStrategyFactory() {
        return strategyFactory;
    }

    // Creates or returns a PENDING payment for bank transfers or 'Pay Later'
    @Transactional
    public Payment createOrGetPendingPayment(Long bookingId, Long customerId, BigDecimal amount) {
        // Step 1: If payment already exists for this booking, check its status
        Optional<Payment> existing = paymentRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            // Step 1a: Do not overwrite an already verified payment
            if (p.getStatus() == PaymentStatus.VERIFIED || p.getStatus() == PaymentStatus.REFUND_REQUESTED) {
                throw new ValidationException("Payment for this booking has already been accepted.");
            }
            p.setStatus(PaymentStatus.PENDING);
            return paymentRepository.save(p);
        }

        // Step 2: Create a temporary reference ID for the bank transfer
        String transactionId = "PAY-LATER-" + System.currentTimeMillis();

        // Step 3: Create and save the payment in PENDING status
        Payment payment = new Payment(bookingId, customerId, amount, PaymentMethod.BANK_TRANSFER, transactionId);
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    // Manually marks a pending payment as VERIFIED (used by Finance staff)
    @Transactional
    public Payment verifyPayment(Long paymentId) {
        // Step 1: Find the payment by ID
        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException("Only payments in PENDING status can be verified.");
        }

        // Step 2: Mark payment as VERIFIED and record timestamp
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setProcessedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        // Step 3: Confirm the booking
        bookingService.confirmBooking(payment.getBookingId());

        // Step 4: Automatically generate an invoice
        invoiceService.generateInvoice(payment.getBookingId());

        // Step 5: Transition linked custom trip request to CONVERTED_TO_BOOKING if present
        if (tripRequestRepository != null) {
            tripRequestRepository.findByBookingId(payment.getBookingId()).ifPresent(req -> {
                req.setStatus(TripRequestStatus.CONVERTED_TO_BOOKING);
                tripRequestRepository.save(req);
            });
        }

        // Step 6: Return the verified payment
        return saved;
    }

    // Updates the bank transfer reference ID on a pending payment
    @Transactional
    public Payment updatePaymentReference(Long paymentId, String transactionId) {
        // Step 1: Find the payment by ID
        Payment payment = getPaymentById(paymentId);

        // Step 2: Ensure only PENDING payments can be edited
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException("Only payments in PENDING status can be edited.");
        }

        // Step 3: Update reference ID if provided
        if (transactionId != null && !transactionId.isBlank()) {
            payment.setTransactionId(transactionId.trim());
        }

        // Step 4: Save and return the updated payment
        return paymentRepository.save(payment);
    }

    // Allows a customer to change payment method on their own pending payment
    @Transactional
    public Payment updatePaymentMethodByCustomer(Long paymentId, Long customerId, PaymentMethod method) {
        // Step 1: Find the payment by ID
        Payment payment = getPaymentById(paymentId);

        // Step 2: Ensure only PENDING payments can be edited
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException("Accepted payments cannot be edited.");
        }

        // Step 3: Check that this payment belongs to the logged-in customer
        if (!payment.getCustomerId().equals(customerId)) {
            throw new ValidationException("You are not authorized to update this payment.");
        }

        // Step 4: Update payment method if provided
        if (method != null) {
            payment.setMethod(method);
        }

        // Step 5: Save and return the updated payment
        return paymentRepository.save(payment);
    }

    // Allows Finance staff to edit reference ID and payment method on a pending payment
    @Transactional
    public Payment updatePaymentDetails(Long paymentId, String transactionId, PaymentMethod method) {
        // Step 1: Find the payment by ID
        Payment payment = getPaymentById(paymentId);

        // Step 2: Ensure only PENDING payments can be edited
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ValidationException("Only payments in PENDING status can be edited.");
        }

        // Step 3: Update transaction reference if provided
        if (transactionId != null && !transactionId.isBlank()) {
            payment.setTransactionId(transactionId.trim());
        }

        // Step 4: Update payment method if provided
        if (method != null) {
            payment.setMethod(method);
        }

        // Step 5: Save and return the updated payment
        return paymentRepository.save(payment);
    }

    // Deletes a payment record if it has not been verified yet
    @Transactional
    public void deletePayment(Long paymentId) {
        // Step 1: Find the payment by ID
        Payment payment = getPaymentById(paymentId);

        // Step 2: Prevent deleting verified or refunded payments
        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            throw new ValidationException("Accepted payments cannot be deleted.");
        }
        if (payment.getStatus() == PaymentStatus.REFUND_REQUESTED) {
            throw new ValidationException("Payments with a pending refund request cannot be deleted.");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ValidationException("Refunded payments cannot be deleted.");
        }

        // Step 2b: Remove or orphan any linked refund records before deleting non-verified payment
        if (refundRepository != null) {
            refundRepository.findByPaymentId(paymentId).ifPresent(refundRepository::delete);
        }

        // Step 3: Delete payment record from database
        paymentRepository.delete(payment);
    }
}
