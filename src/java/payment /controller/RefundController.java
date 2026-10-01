package com.islandtrails.payment.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.dto.RefundRequestDTO;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.service.PaymentService;
import com.islandtrails.payment.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RefundController {

    private final RefundService refundService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PermissionService permissionService;

    public RefundController(RefundService refundService,
                            BookingService bookingService,
                            PaymentService paymentService,
                            PermissionService permissionService) {
        this.refundService = refundService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.permissionService = permissionService;
    }

    @GetMapping("/customer/refund/request")
    public String refundRequestForm(@RequestParam("bookingId") Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        Payment payment = paymentService.getPaymentByBooking(bookingId)
                .orElseThrow(() -> new ValidationException("No verified payment associated with this booking."));

        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setBookingId(bookingId);

        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        model.addAttribute("refundDTO", dto);
        return "payment/refund-request";
    }

    @PostMapping("/customer/refund/request")
    public String submitRefundRequest(@Valid @ModelAttribute("refundDTO") RefundRequestDTO dto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            Booking booking = bookingService.getBookingById(dto.getBookingId());
            Payment payment = paymentService.getPaymentByBooking(dto.getBookingId()).orElse(null);
            model.addAttribute("booking", booking);
            model.addAttribute("payment", payment);
            return "payment/refund-request";
        }

        User customer = permissionService.getCurrentUser().orElseThrow();
        try {
            refundService.requestRefund(dto.getBookingId(), customer.getId(), customer.getName(), dto.getReason());
            redirectAttributes.addFlashAttribute("successMessage", "100% Refund request submitted successfully! A Finance Officer will review and approve it.");
            return "redirect:/customer/bookings";
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/booking/" + dto.getBookingId();
        }
    }
}
