package com.islandtrails.catalog.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.catalog.dto.ReviewFormDTO;
import com.islandtrails.catalog.service.ReviewService;
import com.islandtrails.common.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final PermissionService permissionService;

    public ReviewController(ReviewService reviewService, PermissionService permissionService) {
        this.reviewService = reviewService;
        this.permissionService = permissionService;
    }

    @PostMapping("/customer/review/new")
    public String submitReview(@Valid @ModelAttribute("reviewDTO") ReviewFormDTO dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        User customer = permissionService.getCurrentUser().orElseThrow();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid review input. Please check rating and comment.");
            return "redirect:/customer/bookings";
        }

        try {
            reviewService.addReview(
                    dto.getBookingId(),
                    customer.getId(),
                    customer.getName(),
                    dto.getPackageId(),
                    dto.getRating(),
                    dto.getComment(),
                    null,
                    null
            );
            redirectAttributes.addFlashAttribute("successMessage", "Thank you! Your review has been submitted successfully.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/customer/bookings";
    }

    @PostMapping("/staff/tour-ops/review/{id}/respond")
    public String respondToReview(@PathVariable("id") Long reviewId,
                                  @RequestParam("staffResponse") String staffResponse,
                                  @RequestParam(value = "packageId", required = false) Long packageId,
                                  RedirectAttributes redirectAttributes) {
        if (staffResponse == null || staffResponse.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff response cannot be empty.");
            if (packageId != null) {
                return "redirect:/catalog/package/" + packageId;
            }
            return "redirect:/staff/tour-ops/packages";
        }
        try {
            reviewService.respondToReview(reviewId, staffResponse);
            redirectAttributes.addFlashAttribute("successMessage", "Staff response published.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if (packageId != null) {
            return "redirect:/catalog/package/" + packageId;
        }
        return "redirect:/staff/tour-ops/packages";
    }
}
