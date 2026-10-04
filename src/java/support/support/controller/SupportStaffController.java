package com.islandtrails.support.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.auth.service.UserService;
import com.islandtrails.support.entity.*;
import com.islandtrails.support.service.TicketService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff/support")
public class SupportStaffController {

    private final TicketService ticketService;
    private final PermissionService permissionService;
    private final UserService userService;

    public SupportStaffController(TicketService ticketService,
                                  PermissionService permissionService,
                                  UserService userService) {
        this.ticketService = ticketService;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("/tickets")
    public String listStaffTickets(@RequestParam(value = "department", required = false) TicketDepartment department,
                                   @RequestParam(value = "status", required = false) TicketStatus status,
                                   Model model) {
        List<Ticket> tickets;
        if (department != null) {
            tickets = ticketService.getTicketsByDepartment(department);
        } else if (status != null) {
            tickets = ticketService.getTicketsByStatus(status);
        } else {
            tickets = ticketService.getAllTickets();
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("currentDept", department);
        model.addAttribute("currentStatus", status);
        model.addAttribute("departments", TicketDepartment.values());
        model.addAttribute("statuses", TicketStatus.values());
        return "support/staff-tickets";
    }

    @GetMapping("/ticket/{id}")
    public String staffTicketDetail(@PathVariable("id") Long id, Model model) {
        Ticket ticket = ticketService.getTicketById(id);
        List<TicketReply> replies = ticketService.getReplies(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", replies);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
        return "support/staff-ticket-detail";
    }

    @PostMapping("/ticket/{id}/reply")
    public String staffReply(@PathVariable("id") Long id,
                             @RequestParam("message") String message,
                             RedirectAttributes redirectAttributes) {
        User staff = permissionService.getCurrentUser().orElseThrow();
        ticketService.addReply(id, staff.getId(), staff.getName() + " (" + staff.getRole().name() + ")", message, true, null);
        redirectAttributes.addFlashAttribute("successMessage", "Staff response submitted and ticket updated to IN_PROGRESS.");
        return "redirect:/staff/support/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/status")
    public String updateTicketStatus(@PathVariable("id") Long id,
                                     @RequestParam("status") TicketStatus status,
                                     RedirectAttributes redirectAttributes) {
        ticketService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Ticket status updated to " + status.name());
        return "redirect:/staff/support/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/priority")
    public String updateTicketPriority(@PathVariable("id") Long id,
                                       @RequestParam("priority") TicketPriority priority,
                                       RedirectAttributes redirectAttributes) {
        ticketService.updatePriority(id, priority);
        redirectAttributes.addFlashAttribute("successMessage", "Ticket priority updated to " + priority.name());
        return "redirect:/staff/support/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/assign")
    public String assignTicketToSelf(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User staff = permissionService.getCurrentUser().orElseThrow();
        ticketService.assignTicket(id, staff.getId(), staff.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Ticket assigned to you.");
        return "redirect:/staff/support/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/spam")
    public String markSpam(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        ticketService.markAsSpam(id);
        redirectAttributes.addFlashAttribute("warningMessage", "Ticket marked as SPAM.");
        return "redirect:/staff/support/tickets";
    }
}
