package com.islandtrails.tripplanning.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.auth.service.SystemConfigService;
import com.islandtrails.booking.entity.Booking;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.service.ResourceService;
import com.islandtrails.tripplanning.entity.Quotation;
import com.islandtrails.tripplanning.entity.QuotationLine;
import com.islandtrails.tripplanning.entity.TripRequest;
import com.islandtrails.tripplanning.service.QuotationService;
import com.islandtrails.tripplanning.service.TripRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/staff/consultant")
public class ConsultantController {

    private final TripRequestService tripRequestService;
    private final QuotationService quotationService;
    private final ResourceService resourceService;
    private final PermissionService permissionService;
    private final SystemConfigService systemConfigService;

    public ConsultantController(TripRequestService tripRequestService,
                                QuotationService quotationService,
                                ResourceService resourceService,
                                PermissionService permissionService,
                                SystemConfigService systemConfigService) {
        this.tripRequestService = tripRequestService;
        this.quotationService = quotationService;
        this.resourceService = resourceService;
        this.permissionService = permissionService;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/trip-requests")
    public String listRequests(Model model) {
        model.addAttribute("requests", tripRequestService.getAllTripRequests());
        return "tripplanning/consultant-requests";
    }

    @GetMapping("/trip-request/{id}")
    public String tripRequestDetail(@PathVariable("id") Long id, Model model) {
        TripRequest request = tripRequestService.getTripRequestById(id);
        List<Quotation> existingQuotations = quotationService.getQuotationsForTripRequest(id);
        List<Resource> availableResources = resourceService.getActiveResources();
        BigDecimal defaultMargin = systemConfigService.getProfitMarginPercentage();

        model.addAttribute("tripRequest", request);
        model.addAttribute("quotations", existingQuotations);
        model.addAttribute("resources", availableResources);
        model.addAttribute("defaultMargin", defaultMargin);
        return "tripplanning/quotation-builder";
    }

    @PostMapping("/quotation/create")
    public String createQuotation(@RequestParam("tripRequestId") Long tripRequestId,
                                  @RequestParam(value = "profitMarginPercent", required = false) BigDecimal profitMargin,
                                  @RequestParam(value = "resourceIds", required = false) List<Long> resourceIds,
                                  @RequestParam(value = "quantities", required = false) List<Integer> quantities,
                                  @RequestParam(value = "descriptions", required = false) List<String> descriptions,
                                  @RequestParam(value = "itineraryNotes", required = false) String itineraryNotes,
                                  RedirectAttributes redirectAttributes) {

        User consultant = permissionService.getCurrentUser().orElseThrow();
        List<QuotationLine> lines = new ArrayList<>();

        if (resourceIds != null && quantities != null) {
            for (int i = 0; i < resourceIds.size(); i++) {
                Long rId = resourceIds.get(i);
                int qty = (i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : 1;
                String desc = (descriptions != null && i < descriptions.size()) ? descriptions.get(i) : "";

                Resource res = resourceService.getResourceById(rId);
                QuotationLine line = new QuotationLine(res.getId(), res.getName(), res.getResourceType().name(), qty, res.getUnitCost(), i + 1, desc);
                lines.add(line);
            }
        }

        try {
            Quotation quotation = quotationService.createQuotation(
                    tripRequestId,
                    consultant.getId(),
                    consultant.getName(),
                    profitMargin,
                    lines,
                    itineraryNotes
            );
            redirectAttributes.addFlashAttribute("successMessage", "Quotation v" + quotation.getQuotationVersion() + " created and sent to customer successfully.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/consultant/trip-request/" + tripRequestId;
    }

    @PostMapping("/quotation/{id}/revise")
    public String reviseQuotation(@PathVariable("id") Long quotationId,
                                  @RequestParam(value = "profitMarginPercent", required = false) BigDecimal profitMargin,
                                  @RequestParam(value = "resourceIds", required = false) List<Long> resourceIds,
                                  @RequestParam(value = "quantities", required = false) List<Integer> quantities,
                                  @RequestParam(value = "descriptions", required = false) List<String> descriptions,
                                  @RequestParam(value = "itineraryNotes", required = false) String itineraryNotes,
                                  RedirectAttributes redirectAttributes) {

        User consultant = permissionService.getCurrentUser().orElseThrow();
        List<QuotationLine> lines = new ArrayList<>();

        if (resourceIds != null && quantities != null) {
            for (int i = 0; i < resourceIds.size(); i++) {
                Long rId = resourceIds.get(i);
                int qty = (i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : 1;
                String desc = (descriptions != null && i < descriptions.size()) ? descriptions.get(i) : "";

                Resource res = resourceService.getResourceById(rId);
                QuotationLine line = new QuotationLine(res.getId(), res.getName(), res.getResourceType().name(), qty, res.getUnitCost(), i + 1, desc);
                lines.add(line);
            }
        }

        try {
            Quotation revision = quotationService.reviseQuotation(
                    quotationId,
                    consultant.getId(),
                    consultant.getName(),
                    profitMargin,
                    lines,
                    itineraryNotes
            );
            redirectAttributes.addFlashAttribute("successMessage", "Quotation revised to version " + revision.getQuotationVersion() + ". Previous version marked SUPERSEDED.");
            return "redirect:/staff/consultant/trip-request/" + revision.getTripRequestId();
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/staff/consultant/trip-requests";
        }
    }

    @PostMapping("/quotation/{id}/convert")
    public String convertToBooking(@PathVariable("id") Long quotationId, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = quotationService.convertQuotationToBooking(quotationId);
            redirectAttributes.addFlashAttribute("successMessage", "Quotation converted to Booking #" + booking.getId() + " and Package auto-created!");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/consultant/trip-requests";
    }
}
