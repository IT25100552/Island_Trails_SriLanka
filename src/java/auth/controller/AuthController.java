package com.islandtrails.auth.controller;

import com.islandtrails.auth.dto.RegisterDTO;
import com.islandtrails.auth.service.UserService;
import com.islandtrails.common.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "session", required = false) String session,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (session != null) {
            model.addAttribute("infoMessage", "Session expired. Please log in again.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerCustomer(dto.getEmail(), dto.getName(), dto.getPassword(), dto.getPhoneNumber());
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! You can now log in to your account.");
            return "redirect:/auth/login";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/password-reset")
    public String passwordResetPage() {
        return "auth/password-reset";
    }

    @PostMapping("/password-reset")
    public String handlePasswordReset(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("infoMessage", "If an active account exists for " + email + ", password reset instructions have been dispatched.");
        return "redirect:/auth/login";
    }
}
