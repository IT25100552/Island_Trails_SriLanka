package com.islandtrails.auth.controller;

import com.islandtrails.auth.dto.StaffUserDTO;
import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import com.islandtrails.auth.service.AuditService;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.auth.service.SystemConfigService;
import com.islandtrails.auth.service.UserService;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.catalog.repository.PackageRepository;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.payment.repository.PaymentRepository;
import com.islandtrails.support.repository.TicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final SystemConfigService systemConfigService;
    private final BookingRepository bookingRepository;
    private final PackageRepository packageRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    public AdminController(UserService userService,
                           PermissionService permissionService,
                           AuditService auditService,
                           SystemConfigService systemConfigService,
                           BookingRepository bookingRepository,
                           PackageRepository packageRepository,
                           PaymentRepository paymentRepository,
                           TicketRepository ticketRepository) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.systemConfigService = systemConfigService;
        this.bookingRepository = bookingRepository;
        this.packageRepository = packageRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        User admin = permissionService.getCurrentUser().orElseThrow();
        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalPackages", packageRepository.count());
        model.addAttribute("totalBookings", bookingRepository.count());
        model.addAttribute("totalPayments", paymentRepository.count());
        model.addAttribute("totalTickets", ticketRepository.count());
        model.addAttribute("recentLogs", auditService.getAllAuditLogs().stream().limit(10).toList());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("allRoles", UserRole.values());
        return "admin/users";
    }

    @GetMapping("/user/new")
    public String newStaffUserForm(Model model) {
        model.addAttribute("staffUserDTO", new StaffUserDTO());
        model.addAttribute("staffRoles", List.of(
                UserRole.TOUR_OPS_MANAGER,
                UserRole.TRAVEL_CONSULTANT,
                UserRole.FINANCE_OFFICER,
                UserRole.CUSTOMER_RELATIONS_OFFICER,
                UserRole.IT_SYSTEMS_OFFICER
        ));
        return "admin/user-form";
    }

    @PostMapping("/user/new")
    public String createStaffUser(@Valid @ModelAttribute("staffUserDTO") StaffUserDTO dto,
                                  BindingResult bindingResult,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("staffRoles", List.of(
                    UserRole.TOUR_OPS_MANAGER,
                    UserRole.TRAVEL_CONSULTANT,
                    UserRole.FINANCE_OFFICER,
                    UserRole.CUSTOMER_RELATIONS_OFFICER,
                    UserRole.IT_SYSTEMS_OFFICER
            ));
            return "admin/user-form";
        }

        User admin = permissionService.getCurrentUser().orElseThrow();
        try {
            userService.createStaffUser(
                    dto.getEmail(),
                    dto.getName(),
                    dto.getPassword(),
                    dto.getRole(),
                    dto.getPhoneNumber(),
                    admin.getId(),
                    admin.getEmail(),
                    request.getRemoteAddr()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Staff account created successfully for " + dto.getEmail());
            return "redirect:/admin/users";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("staffRoles", List.of(
                    UserRole.TOUR_OPS_MANAGER,
                    UserRole.TRAVEL_CONSULTANT,
                    UserRole.FINANCE_OFFICER,
                    UserRole.CUSTOMER_RELATIONS_OFFICER,
                    UserRole.IT_SYSTEMS_OFFICER
            ));
            return "admin/user-form";
        }
    }

    @PostMapping("/user/{id}/role")
    public String updateRole(@PathVariable("id") Long id,
                             @RequestParam("role") UserRole newRole,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User admin = permissionService.getCurrentUser().orElseThrow();
        userService.updateStaffRole(id, newRole, admin.getId(), admin.getEmail(), request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully to " + newRole.name());
        return "redirect:/admin/users";
    }

    @PostMapping("/user/{id}/deactivate")
    public String deactivateUser(@PathVariable("id") Long id,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User admin = permissionService.getCurrentUser().orElseThrow();
        userService.deactivateUser(id, admin.getId(), admin.getEmail(), request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("warningMessage", "User account deactivated (soft-deleted).");
        return "redirect:/admin/users";
    }

    @PostMapping("/user/{id}/reactivate")
    public String reactivateUser(@PathVariable("id") Long id,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User admin = permissionService.getCurrentUser().orElseThrow();
        userService.reactivateUser(id, admin.getId(), admin.getEmail(), request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("successMessage", "User account reactivated successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/audit-log")
    public String viewAuditLog(Model model) {
        model.addAttribute("logs", auditService.getAllAuditLogs());
        return "admin/audit-log";
    }

    @GetMapping("/settings")
    public String viewSettings(Model model) {
        model.addAttribute("configs", systemConfigService.getAllConfigs());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSetting(@RequestParam("key") String key,
                                @RequestParam("value") String value,
                                @RequestParam(value = "description", required = false) String description,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Setting key and value cannot be empty.");
            return "redirect:/admin/settings";
        }
        User admin = permissionService.getCurrentUser().orElseThrow();
        systemConfigService.setConfig(key, value, description, admin.getId(), admin.getEmail(), request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("successMessage", "System setting '" + key + "' updated successfully.");
        return "redirect:/admin/settings";
    }
}
