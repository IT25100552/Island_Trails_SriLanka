package com.islandtrails.payment.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.payment.entity.Payment;
import com.islandtrails.payment.entity.Refund;
import com.islandtrails.payment.service.InvoiceService;
import com.islandtrails.payment.service.PaymentService;
import com.islandtrails.payment.service.RefundService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff/finance")
public class FinanceController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final InvoiceService invoiceService;
    private final PermissionService permissionService;

    public FinanceController(PaymentService paymentService,
                             RefundService refundService,
                             InvoiceService invoiceService,
                             PermissionService permissionService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.invoiceService = invoiceService;
        this.permissionService = permissionService;
    }

    @GetMapping("/payments")
    public String viewPayments(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        model.addAttribute("payments", payments);
        return "payment/finance-payments";
    }

    @GetMapping("/refunds")
    public String viewRefunds(Model model) {
        List<Refund> refunds = refundService.getAllRefunds();
        model.addAttribute("refunds", refunds);
        return "payment/finance-refunds";
    }

    @PostMapping("/refund/{id}/approve")
    public String approveRefund(@PathVariable("id") Long id,
                                @RequestParam(value = "decisionReason", required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        User financeOfficer = permissionService.getCurrentUser().orElseThrow();
        Refund refund = refundService.approveRefund(id, financeOfficer.getId(), financeOfficer.getName(), reason != null ? reason : "Approved 100% refund by Finance");
        redirectAttributes.addFlashAttribute("successMessage", "100% Refund approved successfully! Code: " + refund.getRefundConfirmationCode());
        return "redirect:/staff/finance/refunds";
    }

    @PostMapping("/refund/{id}/reject")
    public String rejectRefund(@PathVariable("id") Long id,
                               @RequestParam(value = "decisionReason", required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        User financeOfficer = permissionService.getCurrentUser().orElseThrow();
        refundService.rejectRefund(id, financeOfficer.getId(), financeOfficer.getName(), reason != null ? reason : "Rejected by Finance Officer");
        redirectAttributes.addFlashAttribute("infoMessage", "Refund request has been rejected.");
        return "redirect:/staff/finance/refunds";
    }

    @GetMapping("/reports")
    public String financialReport(Model model) {
        Map<String, Object> report = refundService.generateFinancialReport();
        model.addAllAttributes(report);
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "payment/financial-report";
    }

    @PostMapping("/invoice/{id}/void")
    public String voidInvoice(@PathVariable("id") Long invoiceId, RedirectAttributes redirectAttributes) {
        invoiceService.voidInvoice(invoiceId);
        redirectAttributes.addFlashAttribute("warningMessage", "Invoice marked as VOID.");
        return "redirect:/staff/finance/reports";
    }
}
