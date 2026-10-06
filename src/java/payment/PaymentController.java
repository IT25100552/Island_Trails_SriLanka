package com.islandtrails.payment.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.DuplicatePaymentException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.dto.PaymentFormDTO;
import com.islandtrails.payment.entity.Invoice;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.PaymentMethod;
import com.islandtrails.payment.entity.PaymentStatus;
import com.islandtrails.payment.service.InvoiceService;
import com.islandtrails.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Controller for handling customer payments, checkout, and invoice viewing
@Controller
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final PermissionService permissionService;

    // Injects required services for payments, bookings, invoices, and permissions
    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             InvoiceService invoiceService,
                             PermissionService permissionService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.permissionService = permissionService;
    }

    // Displays the payment checkout form for a specific booking
    @GetMapping("/customer/booking/{id}/payment")
    public String paymentCheckoutPage(@PathVariable("id") Long bookingId, Model model, RedirectAttributes redirectAttributes) {
        // Step 1: Get the currently logged-in customer
        User customer = permissionService.getCurrentUser().orElse(null);
        try {
            // Step 2: Look up the booking by ID
            Booking booking = bookingService.getBookingById(bookingId);

            // Step 3: Check that the customer owns this booking
            if (customer != null && !booking.getCustomerId().equals(customer.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to view payment for this booking.");
                return "redirect:/customer/dashboard";
            }

            // Step 4: Check if the booking is already confirmed and paid
            if (booking.getStatus() == com.islandtrails.booking.entity.BookingStatus.CONFIRMED) {
                redirectAttributes.addFlashAttribute("infoMessage", "Booking #" + bookingId + " has already been paid and confirmed.");
                return "redirect:/customer/booking/" + bookingId;
            }

            // Step 5: Set up the payment form with default values
            PaymentFormDTO dto = new PaymentFormDTO();
            dto.setBookingId(bookingId);
            dto.setMethod(PaymentMethod.CREDIT_CARD);

            // Step 6: Add attributes to model and display the payment form
            model.addAttribute("booking", booking);
            model.addAttribute("paymentDTO", dto);
            model.addAttribute("paymentMethods", List.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD));
            return "payment/payment-form";
        } catch (com.islandtrails.common.exception.ResourceNotFoundException ex) {
            // Step 7: Clean up any orphaned payment record if booking is missing
            paymentService.getPaymentByBooking(bookingId).ifPresent(p -> {
                try { paymentService.deletePayment(p.getId()); } catch (Exception ignored) {}
            });
            redirectAttributes.addFlashAttribute("errorMessage", "Booking #" + bookingId + " could not be found or has already been cancelled.");
            return "redirect:/customer/dashboard";
        }
    }

    // Processes payment submission from the checkout form
    @PostMapping("/customer/booking/{id}/payment")
    public String processPayment(@PathVariable("id") Long bookingId,
                                 @Valid @ModelAttribute("paymentDTO") PaymentFormDTO dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        // Step 1: Look up booking details for pricing
        Booking booking = bookingService.getBookingById(bookingId);

        // Step 2: If form has errors, re-render the form
        if (bindingResult.hasErrors()) {
            model.addAttribute("booking", booking);
            model.addAttribute("paymentMethods", List.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD));
            return "payment/payment-form";
        }

        // Step 3: Retrieve the logged-in customer
        User customer = permissionService.getCurrentUser().orElseThrow();

        // Step 3a: Verify booking ownership and status
        if (!booking.getCustomerId().equals(customer.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to pay for this booking.");
            return "redirect:/customer/dashboard";
        }
        if (booking.getStatus() != com.islandtrails.booking.entity.BookingStatus.PENDING_PAYMENT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment cannot be processed because this booking is " + booking.getStatus() + ".");
            return "redirect:/customer/booking/" + bookingId;
        }

        try {
            // Step 4: Process the payment through PaymentService
            Payment payment = paymentService.makePayment(
                    bookingId,
                    customer.getId(),
                    booking.getTotalPrice(),
                    dto.getMethod(),
                    dto.getCardNumber()
            );

            // Step 5: Show message and redirect based on payment outcome
            if (payment.getStatus() == PaymentStatus.DECLINED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Payment was DECLINED by simulated gateway. Please try another card.");
                return "redirect:/customer/booking/" + bookingId + "/payment";
            } else if (payment.getStatus() == PaymentStatus.VERIFIED) {
                redirectAttributes.addFlashAttribute("successMessage", "Payment verified successfully! Your booking is now CONFIRMED. Transaction ID: " + payment.getTransactionId());
                return "redirect:/customer/booking/" + bookingId;
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Payment submitted successfully and is pending verification by Finance. Transaction ID: " + payment.getTransactionId());
                return "redirect:/customer/booking/" + bookingId;
            }
        } catch (DuplicatePaymentException ex) {
            // Step 6: Handle duplicate payment attempts
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/booking/" + bookingId;
        }
    }

    // Redirects customer payment listing to the customer dashboard
    @GetMapping("/customer/payments")
    public String listPayments() {
        return "redirect:/customer/dashboard";
    }

    // Displays invoice details by invoice ID
    @GetMapping({"/customer/invoice/{id}", "/invoice/{id}"})
    public String viewInvoice(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        // Step 1: Look up the invoice by ID
        Invoice invoice = invoiceService.getInvoiceById(id);

        // Step 2: Fetch corresponding booking and payment records
        Booking booking = bookingService.getBookingById(invoice.getBookingId());

        // Step 2a: Enforce customer ownership check
        User currentUser = permissionService.getCurrentUser().orElse(null);
        if (currentUser != null && currentUser.getRole() == UserRole.CUSTOMER && !booking.getCustomerId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to view this invoice.");
            return "redirect:/customer/dashboard";
        }

        Payment payment = paymentService.getPaymentByBooking(invoice.getBookingId()).orElse(null);

        // Step 3: Populate model and render invoice view
        model.addAttribute("invoice", invoice);
        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        return "payment/invoice-detail";
    }

    // Displays invoice details by booking ID
    @GetMapping({"/customer/booking/{id}/invoice", "/booking/{id}/invoice"})
    public String viewInvoiceByBooking(@PathVariable("id") Long bookingId, Model model, RedirectAttributes redirectAttributes) {
        // Step 1: Look up the invoice for the given booking
        Invoice invoice = invoiceService.getInvoiceByBooking(bookingId)
                .orElseThrow(() -> new com.islandtrails.common.exception.ResourceNotFoundException("Invoice not found for booking: " + bookingId));

        // Step 2: Fetch corresponding booking and payment records
        Booking booking = bookingService.getBookingById(bookingId);

        // Step 2a: Enforce customer ownership check
        User currentUser = permissionService.getCurrentUser().orElse(null);
        if (currentUser != null && currentUser.getRole() == UserRole.CUSTOMER && !booking.getCustomerId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to view this invoice.");
            return "redirect:/customer/dashboard";
        }

        Payment payment = paymentService.getPaymentByBooking(bookingId).orElse(null);

        // Step 3: Populate model and render invoice view
        model.addAttribute("invoice", invoice);
        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        return "payment/invoice-detail";
    }

    // Handles the 'Pay Later' / bank transfer payment selection
    @PostMapping("/customer/booking/{id}/pay-later")
    public String payLater(@PathVariable("id") Long bookingId, RedirectAttributes redirectAttributes) {
        // Step 1: Look up the booking and authenticated user
        Booking booking = bookingService.getBookingById(bookingId);
        User customer = permissionService.getCurrentUser().orElseThrow();

        // Step 2: Verify the user owns this booking
        if (!booking.getCustomerId().equals(customer.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission for this booking.");
            return "redirect:/customer/bookings";
        }

        // Step 3: Create a pending payment record
        try {
            Payment payment = paymentService.createOrGetPendingPayment(bookingId, customer.getId(), booking.getTotalPrice());
            redirectAttributes.addFlashAttribute("successMessage", "Pay Later selected! A pending payment record has been created (Reference: " + payment.getTransactionId() + "). You can pay now or transfer at any time.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // Step 4: Redirect to customer dashboard
        return "redirect:/customer/dashboard";
    }

    // Allows a customer to change the payment method for an existing pending payment
    @PostMapping("/customer/payment/{id}/method")
    public String updatePaymentMethod(@PathVariable("id") Long paymentId,
                                      @RequestParam("method") PaymentMethod method,
                                      RedirectAttributes redirectAttributes) {
        // Step 1: Get the authenticated customer
        User customer = permissionService.getCurrentUser().orElseThrow();

        // Step 2: Update the payment method via PaymentService
        try {
            Payment payment = paymentService.updatePaymentMethodByCustomer(paymentId, customer.getId(), method);
            redirectAttributes.addFlashAttribute("successMessage", "Payment method successfully updated to " + method + ".");
            return "redirect:/customer/booking/" + payment.getBookingId();
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/payments";
        }
    }
}
