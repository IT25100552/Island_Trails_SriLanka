package com.islandtrails.payment.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.UnauthorizedException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.dto.RefundRequestDTO;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.Refund;
import com.islandtrails.payment.entity.RefundStatus;
import com.islandtrails.payment.service.PaymentService;
import com.islandtrails.payment.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controller for handling customer refund requests
@Controller
public class RefundController {

    private final RefundService refundService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PermissionService permissionService;

    // Injects required services for refunds, bookings, payments, and permissions
    public RefundController(RefundService refundService,
                            BookingService bookingService,
                            PaymentService paymentService,
                            PermissionService permissionService) {
        this.refundService = refundService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.permissionService = permissionService;
    }

    // Displays the refund request form for a confirmed booking
    @GetMapping("/customer/refund/request")
    public String refundRequestForm(@RequestParam("bookingId") Long bookingId, Model model) {
        // Step 1: Look up booking details and verify customer ownership (IDOR guard)
        User customer = permissionService.getCurrentUser().orElseThrow();
        Booking booking = bookingService.getBookingById(bookingId);
        if (!booking.getCustomerId().equals(customer.getId())) {
            throw new UnauthorizedException("You are not authorized to view or request a refund for this booking.");
        }

        // Step 2: Make sure a verified payment exists for this booking
        Payment payment = paymentService.getPaymentByBooking(bookingId)
                .orElseThrow(() -> new ValidationException("No verified payment associated with this booking."));

        // Step 3: Initialize the refund request form DTO
        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setBookingId(bookingId);

        // Step 4: Add attributes to model and display the refund request form
        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        model.addAttribute("refundDTO", dto);
        addAppealAttributes(model, bookingId, customer.getId());
        return "payment/refund-request";
    }

    // Handles submission of a refund request by a customer
    @PostMapping("/customer/refund/request")
    public String submitRefundRequest(@Valid @ModelAttribute("refundDTO") RefundRequestDTO dto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        // Step 1: Check form validation errors
        if (bindingResult.hasErrors()) {
            Booking booking = bookingService.getBookingById(dto.getBookingId());
            Payment payment = paymentService.getPaymentByBooking(dto.getBookingId()).orElse(null);
            model.addAttribute("booking", booking);
            model.addAttribute("payment", payment);
            User currentUser = permissionService.getCurrentUser().orElseThrow();
            addAppealAttributes(model, dto.getBookingId(), currentUser.getId());
            return "payment/refund-request";
        }

        // Step 2: Retrieve the logged-in customer and enforce booking ownership (IDOR guard)
        User customer = permissionService.getCurrentUser().orElseThrow();
        Booking booking = bookingService.getBookingById(dto.getBookingId());
        if (!booking.getCustomerId().equals(customer.getId())) {
            throw new UnauthorizedException("You are not authorized to request a refund for this booking.");
        }

        // Step 3: Submit the refund request via RefundService
        try {
            refundService.requestRefund(dto.getBookingId(), customer.getId(), customer.getName(), dto.getReason());
            redirectAttributes.addFlashAttribute("successMessage", "100% Refund request submitted successfully! A Finance Officer will review and approve it.");
            return "redirect:/customer/dashboard";
        } catch (ValidationException ex) {
            // Step 4: Handle validation exceptions and show error message
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    // Lets a customer withdraw their own refund request while it is still pending review
    @PostMapping("/customer/refund/{id}/withdraw")
    public String withdrawRefundRequest(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Identify the logged-in customer (ownership is enforced in the service)
        User customer = permissionService.getCurrentUser().orElseThrow();

        // Step 2: Withdraw and report the outcome
        try {
            refundService.withdrawRefundRequest(id, customer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Refund request withdrawn. Your booking remains confirmed.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/dashboard";
    }

    // Adds isAppeal / previousRejectionReason so the template never sees null
    private void addAppealAttributes(Model model, Long bookingId, Long customerId) {
        Refund rejected = refundService.getRefundsByCustomer(customerId).stream()
                .filter(r -> bookingId.equals(r.getBookingId()) && r.getStatus() == RefundStatus.REJECTED)
                .findFirst() // list is ordered by requestedAt desc
                .orElse(null);
        model.addAttribute("isAppeal", rejected != null);
        model.addAttribute("previousRejectionReason", rejected != null ? rejected.getDecisionReason() : null);
    }
}
