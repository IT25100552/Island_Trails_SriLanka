package com.islandtrails.tripplanning.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.tripplanning.dto.TripRequestFormDTO;
import com.islandtrails.tripplanning.entity.Quotation;
import com.islandtrails.tripplanning.entity.TripRequest;
import com.islandtrails.tripplanning.service.QuotationService;
import com.islandtrails.tripplanning.service.TripRequestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class TripRequestController {

    private final TripRequestService tripRequestService;
    private final QuotationService quotationService;
    private final PermissionService permissionService;

    public TripRequestController(TripRequestService tripRequestService,
                                 QuotationService quotationService,
                                 PermissionService permissionService) {
        this.tripRequestService = tripRequestService;
        this.quotationService = quotationService;
        this.permissionService = permissionService;
    }

    @GetMapping("/customer/trip-request/new")
    public String newTripRequestForm(Model model) {
        model.addAttribute("tripRequestDTO", new TripRequestFormDTO());
        return "tripplanning/request-form";
    }

    @PostMapping("/customer/trip-request/new")
    public String submitTripRequest(@Valid @ModelAttribute("tripRequestDTO") TripRequestFormDTO dto,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date must be on or after start date.");
            }
        }

        if (bindingResult.hasErrors()) {
            return "tripplanning/request-form";
        }

        User customer = permissionService.getCurrentUser().orElseThrow();
        try {
            tripRequestService.submitTripRequest(
                    customer.getId(),
                    customer.getName(),
                    dto.getBudget(),
                    dto.getStartDate(),
                    dto.getEndDate(),
                    dto.getInterests(),
                    dto.getSpecialRequirements(),
                    dto.getNumberOfTravelers()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Your custom trip request has been submitted! A travel consultant will review it shortly.");
            return "redirect:/customer/trip-requests";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "tripplanning/request-form";
        }
    }

    @GetMapping("/customer/trip-requests")
    public String listCustomerTripRequests(Model model) {
        User customer = permissionService.getCurrentUser().orElseThrow();
        List<TripRequest> requests = tripRequestService.getRequestsByCustomer(customer.getId());
        model.addAttribute("requests", requests);
        return "tripplanning/customer-requests";
    }

    @GetMapping("/customer/quotation/{id}")
    public String viewQuotation(@PathVariable("id") Long id, Model model) {
        Quotation quotation = quotationService.getQuotationById(id);
        TripRequest request = tripRequestService.getTripRequestById(quotation.getTripRequestId());
        List<Quotation> allVersions = quotationService.getQuotationsForTripRequest(request.getId());

        model.addAttribute("quotation", quotation);
        model.addAttribute("tripRequest", request);
        model.addAttribute("allVersions", allVersions);
        return "tripplanning/quotation-detail";
    }

    @PostMapping("/customer/quotation/{id}/accept")
    public String acceptQuotation(@PathVariable("id") Long quotationId, RedirectAttributes redirectAttributes) {
        quotationService.acceptQuotation(quotationId);
        redirectAttributes.addFlashAttribute("successMessage", "Quotation accepted! The consultant will now confirm and convert it to your official booking.");
        return "redirect:/customer/quotation/" + quotationId;
    }

    @PostMapping("/customer/quotation/{id}/reject")
    public String rejectQuotation(@PathVariable("id") Long quotationId,
                                  @RequestParam(value = "reason", required = false) String reason,
                                  RedirectAttributes redirectAttributes) {
        quotationService.rejectQuotation(quotationId, reason);
        redirectAttributes.addFlashAttribute("infoMessage", "Quotation declined. You may submit a new trip request or request changes.");
        return "redirect:/customer/trip-requests";
    }
}
