package com.islandtrails.payment.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.PaymentMethod;
import com.islandtrails.payment.entity.PaymentStatus;
import com.islandtrails.payment.entity.Refund;
import com.islandtrails.payment.service.InvoiceService;
import com.islandtrails.payment.service.PaymentService;
import com.islandtrails.payment.service.RefundService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.payment.entity.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

// Controller for Finance staff to manage payments, refund approvals, and view financial reports
@Controller
@RequestMapping("/staff/finance")
public class FinanceController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final InvoiceService invoiceService;
    private final PermissionService permissionService;
    private final BookingService bookingService;

    // Injects required services for payments, refunds, invoices, permissions, and booking details
    @Autowired
    public FinanceController(PaymentService paymentService,
                             RefundService refundService,
                             InvoiceService invoiceService,
                             PermissionService permissionService,
                             BookingService bookingService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.invoiceService = invoiceService;
        this.permissionService = permissionService;
        this.bookingService = bookingService;
    }

    public FinanceController(PaymentService paymentService,
                             RefundService refundService,
                             InvoiceService invoiceService,
                             PermissionService permissionService) {
        this(paymentService, refundService, invoiceService, permissionService, null);
    }

    // Displays the list of payment transactions split into Active, Refunded, and Failed tabs
    @GetMapping("/payments")
    public String viewPayments(Model model) {
        // Step 1: Retrieve transactions categorized by status via PaymentService
        List<Payment> activePayments = paymentService.getActivePayments();
        List<Payment> refundedPayments = paymentService.getRefundedPayments();
        List<Payment> failedPayments = paymentService.getFailedPayments();

        // Step 2: Add collections to the model
        model.addAttribute("activePayments", activePayments);
        model.addAttribute("refundedPayments", refundedPayments);
        model.addAttribute("failedPayments", failedPayments);
        model.addAttribute("payments", activePayments);

        return "payment/finance-payments";
    }

    // Manually verifies a pending payment, confirms booking, and generates invoice
    @PostMapping("/payment/{id}/verify")
    public String verifyPayment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Step 1: Verify payment through PaymentService
            paymentService.verifyPayment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment #" + id + " verified successfully. Booking is now confirmed!");
        } catch (Exception ex) {
            // Step 2: Show error message if verification fails
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/finance/payments";
    }

    // Displays the edit form for a pending payment
    @GetMapping("/payment/{id}/edit")
    public String editPaymentForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        // Step 1: Look up payment record by ID
        Payment payment = paymentService.getPaymentById(id);

        // Step 2: Ensure only PENDING payments can be edited
        if (payment.getStatus() != com.islandtrails.payment.entity.PaymentStatus.PENDING) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only payments in PENDING status can be edited. Accepted payments cannot be modified.");
            return "redirect:/staff/finance/payments";
        }

        // Step 3: Add payment to model and render the edit page
        model.addAttribute("payment", payment);
        return "payment/payment-edit";
    }

    // Updates the reference transaction ID on a pending payment
    @PostMapping("/payment/{id}/edit")
    public String updatePayment(@PathVariable("id") Long id,
                                @RequestParam("transactionId") String transactionId,
                                RedirectAttributes redirectAttributes) {
        try {
            // Step 1: Update the reference ID via PaymentService
            paymentService.updatePaymentReference(id, transactionId);
            redirectAttributes.addFlashAttribute("successMessage", "Payment #" + id + " reference ID updated successfully.");
        } catch (Exception ex) {
            // Step 2: Catch errors and notify staff
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/finance/payments";
    }

    // Permanently deletes an unverified or pending payment record
    @PostMapping("/payment/{id}/delete")
    public String deletePayment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Step 1: Delete payment via PaymentService
            paymentService.deletePayment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment #" + id + " permanently deleted.");
        } catch (Exception ex) {
            // Step 2: Handle error if payment is already verified
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/finance/payments";
    }

    // Displays all refund requests on the refund management dashboard
    @GetMapping("/refunds")
    public String viewRefunds(Model model) {
        // Step 1: Retrieve all refunds ordered by newest first
        List<Refund> refunds = refundService.getAllRefunds();

        // Step 2: Add refunds to model and display dashboard
        model.addAttribute("refunds", refunds);

        // Step 3: Flag appeals = refunds on a booking that already had an earlier REJECTED refund
        java.util.Set<Long> appealRefundIds = new java.util.HashSet<>();
        for (Refund ref : refunds) {
            boolean earlierRejected = refunds.stream().anyMatch(o ->
                    o.getBookingId() != null && o.getBookingId().equals(ref.getBookingId())
                            && o.getStatus() == com.islandtrails.payment.entity.RefundStatus.REJECTED
                            && o.getId() < ref.getId());
            if (earlierRejected) {
                appealRefundIds.add(ref.getId());
            }
        }
        model.addAttribute("appealRefundIds", appealRefundIds);
        return "payment/finance-refunds";
    }

    // Redirects GET requests for approval to the refunds page
    @GetMapping("/refund/{id}/approve")
    public String approveRefundGet(@PathVariable("id") Long id) {
        return "redirect:/staff/finance/refunds";
    }

    // Approves a customer refund request under the 100% refund policy
    @PostMapping("/refund/{id}/approve")
    public String approveRefund(@PathVariable("id") Long id,
                                @RequestParam(value = "decisionReason", required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            // Step 1: Get the logged-in Finance Officer
            User financeOfficer = permissionService.getCurrentUser().orElseThrow();

            // Step 2: Approve the refund and cancel the booking
            Refund refund = refundService.approveRefund(id, financeOfficer.getId(), financeOfficer.getName(), reason != null ? reason : "Approved 100% refund by Finance");
            redirectAttributes.addFlashAttribute("successMessage", "100% Refund approved successfully! Code: " + refund.getRefundConfirmationCode());
        } catch (Exception ex) {
            // Step 3: Show error message if approval fails
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/finance/refunds";
    }

    // Redirects GET requests for rejection to the refunds page
    @GetMapping("/refund/{id}/reject")
    public String rejectRefundGet(@PathVariable("id") Long id) {
        return "redirect:/staff/finance/refunds";
    }

    // Rejects a customer refund request with explanatory notes
    @PostMapping("/refund/{id}/reject")
    public String rejectRefund(@PathVariable("id") Long id,
                               @RequestParam(value = "decisionReason", required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            // Step 1: Get the logged-in Finance Officer
            User financeOfficer = permissionService.getCurrentUser().orElseThrow();

            // Step 2: Reject the refund via RefundService
            refundService.rejectRefund(id, financeOfficer.getId(), financeOfficer.getName(), reason != null ? reason : "Rejected by Finance Officer");
            redirectAttributes.addFlashAttribute("infoMessage", "Refund request has been rejected.");
        } catch (Exception ex) {
            // Step 3: Show error message if rejection fails
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/finance/refunds";
    }

    // Displays tax invoice details for finance review
    @GetMapping("/invoice/{id}")
    public String viewInvoice(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        Booking booking = bookingService != null ? bookingService.getBookingById(invoice.getBookingId()) : null;
        Payment payment = paymentService.getPaymentByBooking(invoice.getBookingId()).orElse(null);
        model.addAttribute("invoice", invoice);
        model.addAttribute("booking", booking);
        model.addAttribute("payment", payment);
        return "payment/invoice-detail";
    }

    // Displays the financial reconciliation report screen
    @GetMapping({"/reports", "/report"})
    public String financialReport(Model model) {
        // Step 1: Generate summary metrics from RefundService
        Map<String, Object> report = refundService.generateFinancialReport();

        // Step 2: Add metrics and all invoices to the model
        model.addAllAttributes(report);
        model.addAttribute("invoices", invoiceService.getAllInvoices());

        // Step 3: Render the report template
        return "payment/financial-report";
    }

    // Voids an issued invoice by updating its status to VOID
    @PostMapping("/invoice/{id}/void")
    public String voidInvoice(@PathVariable("id") Long invoiceId, RedirectAttributes redirectAttributes) {
        // Step 1: Mark invoice status as VOID via InvoiceService
        invoiceService.voidInvoice(invoiceId);

        // Step 2: Set notification message and redirect to reports
        redirectAttributes.addFlashAttribute("warningMessage", "Invoice marked as VOID.");
        return "redirect:/staff/finance/reports";
    }
}
