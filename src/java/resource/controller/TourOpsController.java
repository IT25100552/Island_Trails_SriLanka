package com.islandtrails.resource.controller;

import com.islandtrails.booking.service.BookingService;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.entity.ResourceType;
import com.islandtrails.resource.service.AvailabilityService;
import com.islandtrails.resource.service.ResourceService;
import com.islandtrails.resource.dto.ResourceFormDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.islandtrails.resource.repository.ResourceAssignmentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/staff/tour-ops")
public class TourOpsController {

    private final ResourceService resourceService;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;
    private final com.islandtrails.catalog.repository.PackageRepository packageRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;

    // Injects services and repository for tour operations
    public TourOpsController(ResourceService resourceService,
                             AvailabilityService availabilityService,
                             BookingService bookingService,
                             com.islandtrails.catalog.repository.PackageRepository packageRepository,
                             ResourceAssignmentRepository resourceAssignmentRepository) {
        this.resourceService = resourceService;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
        this.packageRepository = packageRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
    }

    // Lists resources grouped by active and inactive, with optional type filter
    @GetMapping("/resources")
    public String listResources(@RequestParam(value = "type", required = false) ResourceType type, Model model) {
        // Step 1: Fetch resources (filtered by type if specified)
        List<Resource> allResources;
        if (type != null) {
            allResources = resourceService.getAllResourcesByType(type);
        } else {
            allResources = resourceService.getAllResources();
        }

        // Step 2: Separate resources into active and inactive lists
        List<Resource> activeResources = allResources.stream().filter(r -> r.getStatus() == ResourceStatus.ACTIVE).collect(java.util.stream.Collectors.toList());
        List<Resource> inactiveResources = allResources.stream().filter(r -> r.getStatus() == ResourceStatus.INACTIVE).collect(java.util.stream.Collectors.toList());

        // Step 3: Collect IDs of resources that have assignments and package assignment counts
        Set<Long> assignedResourceIds = new java.util.HashSet<>();
        Map<Long, Long> packageCountByResourceId = new java.util.HashMap<>();
        for (Resource r : allResources) {
            if (resourceAssignmentRepository != null) {
                if (resourceAssignmentRepository.existsByResourceId(r.getId())) {
                    assignedResourceIds.add(r.getId());
                }
                long pkgCount = resourceAssignmentRepository.countDistinctPackagesByResourceId(r.getId());
                packageCountByResourceId.put(r.getId(), pkgCount);
            }
        }

        // Step 4: Add lists and filter options to the model
        model.addAttribute("activeResources", activeResources);
        model.addAttribute("inactiveResources", inactiveResources);
        model.addAttribute("resources", allResources);
        model.addAttribute("assignedResourceIds", assignedResourceIds);
        model.addAttribute("packageCountByResourceId", packageCountByResourceId);
        model.addAttribute("currentType", type);
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resource/resource-list";
    }

    // Shows the blank form to create a new resource
    @GetMapping("/resource/new")
    public String newResourceForm(Model model) {
        // Step 1: Create an empty resource object and DTO
        ResourceFormDTO dto = new ResourceFormDTO();
        model.addAttribute("resourceDTO", dto);
        model.addAttribute("resource", dto);

        // Step 2: Add resource types to model and show form
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resource/resource-form";
    }

    public String showNewResourceForm(Model model) {
        return newResourceForm(model);
    }

    // Handles the form submission to save a new resource
    @PostMapping("/resource/new")
    public String createResource(@Valid @ModelAttribute("resourceDTO") ResourceFormDTO dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resource", dto);
            model.addAttribute("resourceTypes", ResourceType.values());
            return "resource/resource-form";
        }

        // Step 1: Try to save the resource via service
        try {
            Resource resource = dto.toEntity();
            resourceService.createResource(resource);

            // Step 2: Show success message and redirect to list
            redirectAttributes.addFlashAttribute("successMessage", "Resource '" + resource.getName() + "' added successfully.");
            return "redirect:/staff/tour-ops/resources";
        } catch (ValidationException ex) {
            // Step 3: If validation fails, return back to form with error message
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("resource", dto);
            model.addAttribute("resourceTypes", ResourceType.values());
            return "resource/resource-form";
        }
    }

    // Shows the edit form populated with current resource data
    @GetMapping("/resource/{id}/edit")
    public String editResourceForm(@PathVariable("id") Long id, Model model) {
        // Step 1: Find the resource by ID
        Resource resource = resourceService.getResourceById(id);
        ResourceFormDTO dto = ResourceFormDTO.fromEntity(resource);

        // Step 2: Put resource DTO and types into model and show form
        model.addAttribute("resourceDTO", dto);
        model.addAttribute("resource", dto);
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resource/resource-form";
    }

    // Handles updates to an existing resource
    @PostMapping("/resource/{id}/edit")
    public String updateResource(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("resourceDTO") ResourceFormDTO dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resource", dto);
            model.addAttribute("resourceTypes", ResourceType.values());
            return "resource/resource-form";
        }

        // Step 1: Update resource through service
        try {
            Resource resource = dto.toEntity();
            resourceService.updateResource(id, resource);

            // Step 2: Show success message and redirect to list
            redirectAttributes.addFlashAttribute("successMessage", "Resource updated successfully.");
            return "redirect:/staff/tour-ops/resources";
        } catch (ValidationException ex) {
            // Step 3: If validation fails, redisplay form with error message
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("resource", dto);
            model.addAttribute("resourceTypes", ResourceType.values());
            return "resource/resource-form";
        }
    }

    // Deactivates a resource so it cannot be booked
    @PostMapping("/resource/{id}/deactivate")
    public String deactivateResource(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Deactivate the resource
        resourceService.deleteResource(id);

        // Step 2: Show message and redirect to list
        redirectAttributes.addFlashAttribute("warningMessage", "Resource marked as INACTIVE.");
        return "redirect:/staff/tour-ops/resources";
    }

    // Deletes or archives a resource (Staff Tour Ops only)
    @PostMapping("/resource/{id}/delete")
    public String deleteResource(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Attempt hard delete or archive via service
        try {
            boolean hardDeleted = resourceService.hardDeleteResource(id);
            if (hardDeleted) {
                redirectAttributes.addFlashAttribute("successMessage", "Resource permanently deleted from database.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Resource has assignment history and was successfully archived. It has been removed from active management views.");
            }
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // Step 2: Redirect back to resource list
        return "redirect:/staff/tour-ops/resources";
    }

    // Reactivates an inactive resource so it can be scheduled again
    @PostMapping("/resource/{id}/reactivate")
    public String reactivateResource(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Reactivate the resource
        resourceService.reactivateResource(id);

        // Step 2: Show success message and redirect to list
        redirectAttributes.addFlashAttribute("successMessage", "Resource reactivated successfully.");
        return "redirect:/staff/tour-ops/resources";
    }

    // Shows the assignment calendar for resources over a date range
    @GetMapping("/calendar")
    public String resourceCalendar(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   Model model) {
        // Step 1: Default to current date and 1 month ahead if no dates provided
        LocalDate start = (startDate != null) ? startDate : LocalDate.now();
        LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusMonths(1);

        // Step 2: Build a lookup map of resources by ID
        List<Resource> allResources = resourceService.getAllResources();
        Map<Long, Resource> resourceMap = new java.util.HashMap<>();
        for (Resource r : allResources) {
            resourceMap.put(r.getId(), r);
        }

        // Step 3: Build a lookup map of package names by ID
        List<com.islandtrails.catalog.entity.Package> allPackages = packageRepository.findAll();
        Map<Long, String> packageMap = new java.util.HashMap<>();
        for (com.islandtrails.catalog.entity.Package p : allPackages) {
            packageMap.put(p.getId(), p.getName());
        }

        // Step 4: Add calendar data and lookup maps to model
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("assignments", availabilityService.getAssignmentsInDateRange(start, end));
        model.addAttribute("resources", allResources);
        model.addAttribute("resourceMap", resourceMap);
        model.addAttribute("packageMap", packageMap);

        // Step 5: Display calendar view
        return "resource/calendar";
    }

    // Displays the resource utilization report over a date range
    @GetMapping("/reports/utilization")
    public String utilizationReport(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                    @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                    Model model) {
        // Step 1: Default to 1 month back to 1 month forward if not given
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusMonths(1);

        // Step 2: Compute utilization metrics
        Map<String, Object> report = availabilityService.calculateUtilizationReport(start, end);

        // Step 3: Pass report data to view
        model.addAllAttributes(report);
        return "resource/utilization-report";
    }

    // Displays all customer bookings for Tour Operations staff
    @GetMapping({"/bookings", "/all-bookings"})
    public String viewLiveBookings(Model model) {
        // Step 1: Fetch all bookings and add to model
        model.addAttribute("bookings", bookingService.getAllBookings());
        model.addAttribute("currentDate", LocalDate.now());

        // Step 2: Render staff all-bookings view
        return "booking/all-bookings";
    }
}
