package com.islandtrails.payment.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.DuplicatePaymentException;
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

@Controller
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;
    private final PermissionService permissionService;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             InvoiceService invoiceService,
                             PermissionService permissionService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.permissionService = permissionService;
    }

    @GetMapping("/customer/booking/{id}/payment")
    public String paymentCheckoutPage(@PathVariable("id") Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        PaymentFormDTO dto = new PaymentFormDTO();
        dto.setBookingId(bookingId);
        dto.setMethod(PaymentMethod.CREDIT_CARD);

        model.addAttribute("booking", booking);
        model.addAttribute("paymentDTO", dto);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "payment/payment-form";
    }

    @PostMapping("/customer/booking/{id}/payment")
    public String processPayment(@PathVariable("id") Long bookingId,
                                 @Valid @ModelAttribute("paymentDTO") PaymentFormDTO dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        Booking booking = bookingService.getBookingById(bookingId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("booking", booking);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "payment/payment-form";
        }

        User customer = permissionService.getCurrentUser().orElseThrow();
        try {
            Payment payment = paymentService.makePayment(
                    bookingId,
                    customer.getId(),
                    booking.getTotalPrice(),
                    dto.getMethod(),
                    dto.getCardNumber()
            );

            if (payment.getStatus() == PaymentStatus.VERIFIED) {
                redirectAttributes.addFlashAttribute("successMessage", "Payment verified successfully! Your booking is now CONFIRMED. Transaction ID: " + payment.getTransactionId());
                return "redirect:/customer/booking/" + bookingId;
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Payment was DECLINED by simulated gateway. Please try another card.");
                return "redirect:/customer/booking/" + bookingId + "/payment";
            }
        } catch (DuplicatePaymentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/booking/" + bookingId;
        }
    }

    @GetMapping("/customer/payments")
    public String listPayments(Model model) {
        User customer = permissionService.getCurrentUser().orElseThrow();
        model.addAttribute("payments", paymentService.getPaymentsByCustomer(customer.getId()));
        return "payment/customer-payments";
    }

    @GetMapping("/customer/invoice/{id}")
    public String viewInvoice(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        Booking booking = bookingService.getBookingById(invoice.getBookingId());
        Payment payment = paymentService.getPaymentByBooking(invoice.getBookingId()).orElse(null);

        model.addAttribute("invoice", invoice);
        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        return "payment/invoice-detail";
    }
}
