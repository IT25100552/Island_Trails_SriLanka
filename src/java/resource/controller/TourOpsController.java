package com.islandtrails.resource.controller;

import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.entity.ResourceType;
import com.islandtrails.resource.service.AvailabilityService;
import com.islandtrails.resource.service.ResourceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff/tour-ops")
public class TourOpsController {

    private final ResourceService resourceService;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;
    private final com.islandtrails.catalog.repository.PackageRepository packageRepository;

    public TourOpsController(ResourceService resourceService,
                             AvailabilityService availabilityService,
                             BookingService bookingService,
                             com.islandtrails.catalog.repository.PackageRepository packageRepository) {
        this.resourceService = resourceService;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
        this.packageRepository = packageRepository;
    }

    @GetMapping("/resources")
    public String listResources(@RequestParam(value = "type", required = false) ResourceType type, Model model) {
        List<Resource> resources = (type != null) ? resourceService.getResourcesByType(type) : resourceService.getAllResources();
        model.addAttribute("resources", resources);
        model.addAttribute("currentType", type);
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resource/resource-list";
    }

    @GetMapping("/resource/new")
    public String newResourceForm(Model model) {
        model.addAttribute("resource", new Resource());
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resource/resource-form";
    }

    @PostMapping("/resource/new")
    public String createResource(@ModelAttribute("resource") Resource resource,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            resourceService.createResource(resource);
            redirectAttributes.addFlashAttribute("successMessage", "Resource '" + resource.getName() + "' added successfully.");
            return "redirect:/staff/tour-ops/resources";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("resourceTypes", ResourceType.values());
            return "resource/resource-form";
        }
    }

    @GetMapping("/resource/{id}/edit")
    public String editResourceForm(@PathVariable("id") Long id, Model model) {
        Resource resource = resourceService.getResourceById(id);
        model.addAttribute("resource", resource);
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resource/resource-form";
    }

    @PostMapping("/resource/{id}/edit")
    public String updateResource(@PathVariable("id") Long id,
                                 @ModelAttribute("resource") Resource resource,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            resourceService.updateResource(id, resource);
            redirectAttributes.addFlashAttribute("successMessage", "Resource updated successfully.");
            return "redirect:/staff/tour-ops/resources";
        } catch (ValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("resourceTypes", ResourceType.values());
            return "resource/resource-form";
        }
    }

    @PostMapping("/resource/{id}/deactivate")
    public String deactivateResource(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        resourceService.deleteResource(id);
        redirectAttributes.addFlashAttribute("warningMessage", "Resource marked as INACTIVE.");
        return "redirect:/staff/tour-ops/resources";
    }

    @GetMapping("/calendar")
    public String resourceCalendar(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   Model model) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now();
        LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusMonths(1);

        List<Resource> allResources = resourceService.getAllResources();
        Map<Long, Resource> resourceMap = new java.util.HashMap<>();
        for (Resource r : allResources) {
            resourceMap.put(r.getId(), r);
        }

        List<com.islandtrails.catalog.entity.Package> allPackages = packageRepository.findAll();
        Map<Long, String> packageMap = new java.util.HashMap<>();
        for (com.islandtrails.catalog.entity.Package p : allPackages) {
            packageMap.put(p.getId(), p.getName());
        }

        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("assignments", availabilityService.getAssignmentsInDateRange(start, end));
        model.addAttribute("resources", allResources);
        model.addAttribute("resourceMap", resourceMap);
        model.addAttribute("packageMap", packageMap);
        return "resource/calendar";
    }

    @GetMapping("/reports/utilization")
    public String utilizationReport(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                    Model model) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusMonths(1);

        Map<String, Object> report = availabilityService.calculateUtilizationReport(start, end);
        model.addAllAttributes(report);
        return "resource/utilization-report";
    }

    @GetMapping("/bookings")
    public String viewLiveBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "booking/all-bookings";
    }
}
