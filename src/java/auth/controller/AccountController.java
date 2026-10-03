package com.islandtrails.auth.controller;

import com.islandtrails.auth.dto.PasswordChangeDTO;
import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.auth.service.UserService;
import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.service.PaymentService;
import com.islandtrails.support.service.TicketService;
import com.islandtrails.tripplanning.service.TripRequestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final TripRequestService tripRequestService;
    private final TicketService ticketService;

    public AccountController(UserService userService,
                             PermissionService permissionService,
                             BookingService bookingService,
                             PaymentService paymentService,
                             TripRequestService tripRequestService,
                             TicketService ticketService) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.tripRequestService = tripRequestService;
        this.ticketService = ticketService;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(Model model) {
        User user = permissionService.getCurrentUser().orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("bookings", bookingService.getBookingsByCustomer(user.getId()));
        model.addAttribute("payments", paymentService.getPaymentsByCustomer(user.getId()));
        model.addAttribute("tripRequests", tripRequestService.getRequestsByCustomer(user.getId()));
        model.addAttribute("tickets", ticketService.getTicketsByCustomer(user.getId()));
        return "customer/dashboard";
    }

    @GetMapping("/customer/account")
    public String accountPage(Model model) {
        User user = permissionService.getCurrentUser().orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("passwordChangeDTO", new PasswordChangeDTO());
        return "auth/account";
    }

    @PostMapping("/customer/account/profile")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                RedirectAttributes redirectAttributes) {
        User user = permissionService.getCurrentUser().orElseThrow();
        userService.updateProfile(user.getId(), name, phoneNumber);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/customer/account";
    }

    @PostMapping("/customer/account/password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeDTO") PasswordChangeDTO dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        User user = permissionService.getCurrentUser().orElseThrow();
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "auth/account";
        }

        try {
            userService.changePassword(user.getId(), dto.getOldPassword(), dto.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/customer/account";
    }
}
