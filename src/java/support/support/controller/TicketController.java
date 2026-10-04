package com.islandtrails.support.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.support.dto.TicketFormDTO;
import com.islandtrails.support.entity.InquiryType;
import com.islandtrails.support.entity.Ticket;
import com.islandtrails.support.entity.TicketCategory;
import com.islandtrails.support.entity.TicketReply;
import com.islandtrails.support.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class TicketController {

    private final TicketService ticketService;
    private final PermissionService permissionService;

    public TicketController(TicketService ticketService, PermissionService permissionService) {
        this.ticketService = ticketService;
        this.permissionService = permissionService;
    }

    @GetMapping("/customer/support/ticket/new")
    public String newTicketForm(Model model) {
        model.addAttribute("ticketDTO", new TicketFormDTO());
        model.addAttribute("categories", TicketCategory.values());
        model.addAttribute("inquiryTypes", InquiryType.values());
        return "support/submit-ticket";
    }

    @PostMapping("/customer/support/ticket/new")
    public String submitTicket(@Valid @ModelAttribute("ticketDTO") TicketFormDTO dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", TicketCategory.values());
            model.addAttribute("inquiryTypes", InquiryType.values());
            return "support/submit-ticket";
        }

        User customer = permissionService.getCurrentUser().orElseThrow();
        try {
            Ticket ticket = ticketService.createTicket(
                    customer.getId(),
                    customer.getName(),
                    customer.getEmail(),
                    dto.getSubject(),
                    dto.getDescription(),
                    dto.getCategory(),
                    dto.getInquiryType()
            );

            redirectAttributes.addFlashAttribute("successMessage", "Support ticket #" + ticket.getId() + " submitted! It has been routed to our " + ticket.getDepartment().name() + " department.");
            return "redirect:/customer/support/tickets";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", TicketCategory.values());
            model.addAttribute("inquiryTypes", InquiryType.values());
            return "support/submit-ticket";
        }
    }

    @GetMapping("/customer/support/tickets")
    public String listCustomerTickets(Model model) {
        User customer = permissionService.getCurrentUser().orElseThrow();
        List<Ticket> tickets = ticketService.getTicketsByCustomer(customer.getId());
        model.addAttribute("tickets", tickets);
        return "support/customer-tickets";
    }

    @GetMapping("/customer/support/ticket/{id}")
    public String customerTicketDetail(@PathVariable("id") Long id, Model model) {
        Ticket ticket = ticketService.getTicketById(id);
        List<TicketReply> replies = ticketService.getReplies(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", replies);
        return "support/customer-ticket-detail";
    }

    @PostMapping("/customer/support/ticket/{id}/reply")
    public String replyAsCustomer(@PathVariable("id") Long id,
                                  @RequestParam("message") String message,
                                  RedirectAttributes redirectAttributes) {
        User customer = permissionService.getCurrentUser().orElseThrow();
        ticketService.addReply(id, customer.getId(), customer.getName(), message, false, null);
        redirectAttributes.addFlashAttribute("successMessage", "Reply sent.");
        return "redirect:/customer/support/ticket/" + id;
    }
}
