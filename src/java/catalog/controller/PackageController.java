package com.islandtrails.catalog.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.entity.UserRole;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.catalog.dto.PackageFormDTO;
import com.islandtrails.catalog.dto.PackageSearchDTO;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.entity.PackageOrigin;
import com.islandtrails.catalog.entity.PackageStatus;
import com.islandtrails.catalog.service.PackageService;
import com.islandtrails.support.service.ReviewService;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

// Controller for managing tour packages, searching catalog, and viewing package details
@Controller
public class PackageController {

    private final PackageService packageService;
    private final ReviewService reviewService;
    private final PermissionService permissionService;
    private final com.islandtrails.resource.service.ResourceService resourceService;
    private final com.islandtrails.resource.service.ConflictDetectionService conflictDetectionService;
    private final com.islandtrails.resource.repository.ResourceAssignmentRepository resourceAssignmentRepository;
    private final BookingRepository bookingRepository;

    // Constructor injecting services and repositories needed for package controller
    @Autowired
    public PackageController(PackageService packageService,
                             ReviewService reviewService,
                             PermissionService permissionService,
                             com.islandtrails.resource.service.ResourceService resourceService,
                             com.islandtrails.resource.service.ConflictDetectionService conflictDetectionService,
                             com.islandtrails.resource.repository.ResourceAssignmentRepository resourceAssignmentRepository,
                             BookingRepository bookingRepository) {
        this.packageService = packageService;
        this.reviewService = reviewService;
        this.permissionService = permissionService;
        this.resourceService = resourceService;
        this.conflictDetectionService = conflictDetectionService;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.bookingRepository = bookingRepository;
    }

    public PackageController(PackageService packageService,
                             ReviewService reviewService,
                             com.islandtrails.resource.service.ResourceService resourceService,
                             PermissionService permissionService) {
        this(packageService, reviewService, permissionService, resourceService, null, null, null);
    }

    // Handles searching and filtering published tour packages from the catalog
    @GetMapping("/catalog/search")
    public String searchPackages(@ModelAttribute("searchDTO") PackageSearchDTO searchDTO, Model model) {
        // Step 1: Run the search query using filter criteria from the search DTO
        List<Package> results = packageService.searchPackages(
                searchDTO.getKeyword(),
                searchDTO.getDestination(),
                searchDTO.getMinPrice(),
                searchDTO.getMaxPrice(),
                searchDTO.getMaxDuration()
        );

        // Step 2: Add search results and criteria to the model
        model.addAttribute("packages", results);
        model.addAttribute("searchDTO", searchDTO);

        // Step 3: Return the search results page view
        return "catalog/search";
    }

    // Displays package details along with customer reviews and rating statistics
    @GetMapping("/catalog/package/{id}")
    public String packageDetail(@PathVariable("id") Long id, Model model) {
        // Step 1: Fetch package details by ID
        Package pkg = packageService.getPackageById(id);

        // Step 1a: Guard against unauthorized viewing of non-published or custom quotation packages
        boolean isStaff = permissionService.getCurrentUser()
                .map(u -> u.getRole() == UserRole.TOUR_OPS_MANAGER || u.getRole() == UserRole.IT_SYSTEMS_OFFICER || u.getRole() == UserRole.TRAVEL_CONSULTANT)
                .orElse(false);

        if (!isStaff) {
            if (pkg.getStatus() != PackageStatus.PUBLISHED || pkg.getOrigin() != PackageOrigin.BROWSABLE) {
                throw new ResourceNotFoundException("Package not found with id: " + id);
            }
        }

        // Step 2: Fetch reviews, average rating, review count, and assigned public resources for this package
        model.addAttribute("pkg", pkg);
        model.addAttribute("reviews", reviewService.getReviewsForPackage(id));
        model.addAttribute("avgRating", reviewService.getAverageRating(id));
        model.addAttribute("reviewCount", reviewService.getReviewCount(id));
        model.addAttribute("packageResources", resourceService.getPublicResourcesForPackage(id));

        // Step 3: Render the package details view
        return "catalog/package-detail";
    }

    // Returns the image binary data for a package with the correct content type
    @GetMapping("/catalog/package/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> getPackageImage(@PathVariable("id") Long id) {
        // Step 1: Check if the package exists in the database
        Package pkg = packageService.getPackageById(id);

        // Step 2: Check if an image is uploaded for this package
        if (pkg.getImageData() == null || pkg.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        // Step 3: Determine image format (PNG or JPEG)
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (pkg.getImageMimeType() != null && pkg.getImageMimeType().contains("png")) {
            mediaType = MediaType.IMAGE_PNG;
        }

        // Step 4: Return the image bytes with appropriate headers
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                .body(pkg.getImageData());
    }

    // Shows the staff dashboard table of all tour packages
    @GetMapping("/staff/tour-ops/packages")
    public String managePackages(Model model) {
        // Step 1: Retrieve all tour packages from the database
        List<Package> packages = packageService.getAllPackages();
        Set<Long> bookedPackageIds = new java.util.HashSet<>();
        Map<Long, Long> activeBookingsCountByPackageId = new java.util.HashMap<>();
        java.util.List<com.islandtrails.booking.entity.BookingStatus> activeStatuses = java.util.List.of(
                com.islandtrails.booking.entity.BookingStatus.CONFIRMED,
                com.islandtrails.booking.entity.BookingStatus.ONGOING,
                com.islandtrails.booking.entity.BookingStatus.PENDING_PAYMENT
        );
        for (Package p : packages) {
            if (bookingRepository != null) {
                if (bookingRepository.countByPackageId(p.getId()) > 0) {
                    bookedPackageIds.add(p.getId());
                }
                long activeCount = bookingRepository.countByPackageIdAndStatusIn(p.getId(), activeStatuses);
                activeBookingsCountByPackageId.put(p.getId(), activeCount);
            }
        }
        model.addAttribute("packages", packages);
        model.addAttribute("bookedPackageIds", bookedPackageIds);
        model.addAttribute("activeBookingsCountByPackageId", activeBookingsCountByPackageId);

        // Step 2: Render the staff package management view
        return "catalog/manage-packages";
    }

    // Shows the form for creating a new tour package
    @GetMapping("/staff/tour-ops/package/new")
    public String newPackageForm(Model model) {
        // Step 1: Create an empty package form DTO
        PackageFormDTO dto = new PackageFormDTO();

        // Step 2: Load active resources for staff to select
        model.addAttribute("packageDTO", dto);
        model.addAttribute("resources", resourceService.getActiveResources());

        // Step 3: Render the package creation form
        return "catalog/package-form";
    }

    // Processes new package creation and reserves selected tour resources
    @PostMapping("/staff/tour-ops/package/new")
    public String createPackage(@Valid @ModelAttribute("packageDTO") PackageFormDTO dto,
                                BindingResult bindingResult,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        // Step 1: Check for form validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("resources", resourceService.getActiveResources());
            return "catalog/package-form";
        }

        // Step 2: Get the currently logged-in staff user
        User user = permissionService.getCurrentUser().orElseThrow();

        // Step 3: Make sure the end date is not before the start date
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date cannot be before start date.");
                model.addAttribute("resources", resourceService.getActiveResources());
                return "catalog/package-form";
            }

            // Step 4: Check selected resources for schedule conflicts to prevent double-booking
            if (dto.getResourceIds() != null) {
                for (Long rId : dto.getResourceIds()) {
                    com.islandtrails.resource.entity.Resource res = resourceService.getResourceById(rId);
                    if (res.getStatus() != com.islandtrails.resource.entity.ResourceStatus.ACTIVE) {
                        model.addAttribute("errorMessage", "Cannot assign inactive resource: '" + res.getName() + "'. Please activate it first.");
                        model.addAttribute("resources", resourceService.getActiveResources());
                        return "catalog/package-form";
                    }
                    if (conflictDetectionService.hasConflict(rId, dto.getStartDate(), dto.getEndDate())) {
                        model.addAttribute("errorMessage", "Resource Conflict: '" + res.getName() + "' is already reserved/confirmed on " + dto.getStartDate() + " to " + dto.getEndDate() + ". Double booking prevented.");
                        model.addAttribute("resources", resourceService.getActiveResources());
                        return "catalog/package-form";
                    }
                }
            }
        }

        // Step 4b: Validate and store uploaded package image if provided
        MultipartFile fileToUpload = (image != null && !image.isEmpty()) ? image : dto.getImage();
        String imageUrl = null;
        if (fileToUpload != null && !fileToUpload.isEmpty()) {
            try {
                imageUrl = packageService.validateAndStorePackageImage(fileToUpload);
            } catch (ValidationException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
                model.addAttribute("resources", resourceService.getActiveResources());
                return "catalog/package-form";
            }
        }

        // Step 5: Save the new tour package to the database
        Package savedPackage = packageService.createPackage(
                dto.getName(),
                dto.getDestination(),
                dto.getDescription(),
                dto.getBasePrice(),
                dto.getDurationDays(),
                imageUrl,
                null,
                null,
                PackageOrigin.BROWSABLE,
                user.getId(),
                dto.getStatus()
        );

        // Step 6: Reserve selected resources for this package
        if (dto.getResourceIds() != null && !dto.getResourceIds().isEmpty()) {
            java.time.LocalDate start = (dto.getStartDate() != null) ? dto.getStartDate() : java.time.LocalDate.now().plusDays(7);
            java.time.LocalDate end = (dto.getEndDate() != null) ? dto.getEndDate() : start.plusDays(dto.getDurationDays() != null ? dto.getDurationDays() : 3);

            for (Long rId : dto.getResourceIds()) {
                com.islandtrails.resource.entity.ResourceAssignment ra = new com.islandtrails.resource.entity.ResourceAssignment(
                        savedPackage.getId(),
                        rId,
                        start,
                        end,
                        com.islandtrails.resource.entity.ResourceAssignmentStatus.RESERVED
                );
                resourceAssignmentRepository.save(ra);
            }
        }

        // Step 7: Add success message and redirect back to package list
        redirectAttributes.addFlashAttribute("successMessage", "Tour package created and assigned resources successfully reserved in the availability calendar.");
        return "redirect:/staff/tour-ops/packages";
    }

    // REST endpoint returning active resources that are free during the given dates
    @GetMapping("/staff/tour-ops/resources/available")
    @ResponseBody
    public List<com.islandtrails.resource.entity.Resource> getAvailableResources(
            @RequestParam("startDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam("endDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(value = "packageId", required = false) Long packageId) {

        // Step 1: Retrieve all active resources from the database
        List<com.islandtrails.resource.entity.Resource> allActive = resourceService.getActiveResources();

        // Step 2: Return all resources if no date range is provided
        if (startDate == null || endDate == null) {
            return allActive;
        }

        // Step 3: Find resources already assigned to this package so they aren't marked as conflicts
        List<Long> ownResourceIds = (packageId != null) ?
                resourceAssignmentRepository.findByPackageId(packageId).stream()
                        .map(com.islandtrails.resource.entity.ResourceAssignment::getResourceId).toList() : List.of();

        // Step 4: Filter out resources that have conflicts during the selected dates
        return allActive.stream().filter(res -> {
            if (ownResourceIds.contains(res.getId())) {
                return true;
            }
            return !conflictDetectionService.hasConflict(res.getId(), startDate, endDate);
        }).toList();
    }

    // Shows the package edit form pre-filled with existing details and assigned resources
    @GetMapping("/staff/tour-ops/package/{id}/edit")
    public String editPackageForm(@PathVariable("id") Long id, Model model) {
        // Step 1: Fetch the package by ID
        Package pkg = packageService.getPackageById(id);

        // Step 2: Map package data into the form DTO
        PackageFormDTO dto = new PackageFormDTO();
        dto.setId(pkg.getId());
        dto.setName(pkg.getName());
        dto.setDestination(pkg.getDestination());
        dto.setDescription(pkg.getDescription());
        dto.setBasePrice(pkg.getBasePrice());
        dto.setDurationDays(pkg.getDurationDays());
        dto.setStatus(pkg.getStatus());
        dto.setImageUrl(pkg.getImageUrl());

        // Step 3: Load existing resource assignments for this package
        List<com.islandtrails.resource.entity.ResourceAssignment> assignments = resourceAssignmentRepository.findByPackageId(id);
        if (!assignments.isEmpty()) {
            List<Long> assignedResourceIds = assignments.stream().map(com.islandtrails.resource.entity.ResourceAssignment::getResourceId).toList();
            dto.setResourceIds(new java.util.ArrayList<>(assignedResourceIds));
            dto.setStartDate(assignments.get(0).getStartDate());
            dto.setEndDate(assignments.get(0).getEndDate());

            // Step 4: Filter available resources without showing conflicts for currently assigned ones
            java.time.LocalDate s = dto.getStartDate();
            java.time.LocalDate e = dto.getEndDate();
            List<com.islandtrails.resource.entity.Resource> available = resourceService.getActiveResources().stream().filter(res -> {
                if (assignedResourceIds.contains(res.getId())) return true;
                return !conflictDetectionService.hasConflict(res.getId(), s, e);
            }).toList();
            model.addAttribute("resources", available);
        } else {
            model.addAttribute("resources", resourceService.getActiveResources());
        }

        // Step 5: Add attributes to model and render the edit form
        model.addAttribute("packageDTO", dto);
        model.addAttribute("pkg", pkg);
        return "catalog/package-form";
    }

    // Updates package details and re-checks resource assignments for conflicts
    @PostMapping("/staff/tour-ops/package/{id}/edit")
    public String updatePackage(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("packageDTO") PackageFormDTO dto,
                                BindingResult bindingResult,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        // Step 1: Check for validation errors on submitted form data
        if (bindingResult.hasErrors()) {
            Package pkg = packageService.getPackageById(id);
            model.addAttribute("pkg", pkg);
            model.addAttribute("resources", resourceService.getActiveResources());
            return "catalog/package-form";
        }

        // Step 2: Fetch existing package record
        Package existingPkg = packageService.getPackageById(id);

        // Step 3: Validate that end date is not before start date
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date cannot be before start date.");
                model.addAttribute("pkg", existingPkg);
                model.addAttribute("resources", resourceService.getActiveResources());
                return "catalog/package-form";
            }

            // Step 4: Remove unbooked existing assignments before checking conflicts
            List<com.islandtrails.resource.entity.ResourceAssignment> oldAssignments = resourceAssignmentRepository.findByPackageId(id);
            for (com.islandtrails.resource.entity.ResourceAssignment old : oldAssignments) {
                if (old.getBookingId() == null) {
                    resourceAssignmentRepository.delete(old);
                }
            }

            // Step 5: Check if any newly selected resource has a schedule conflict
            if (dto.getResourceIds() != null) {
                for (Long rId : dto.getResourceIds()) {
                    com.islandtrails.resource.entity.Resource res = resourceService.getResourceById(rId);
                    if (res.getStatus() != com.islandtrails.resource.entity.ResourceStatus.ACTIVE) {
                        model.addAttribute("errorMessage", "Cannot assign inactive resource: '" + res.getName() + "'. Please activate it first.");
                        model.addAttribute("pkg", existingPkg);
                        model.addAttribute("resources", resourceService.getActiveResources());
                        return "catalog/package-form";
                    }
                    if (conflictDetectionService.hasConflict(rId, dto.getStartDate(), dto.getEndDate())) {
                        model.addAttribute("errorMessage", "Resource Conflict: '" + res.getName() + "' is already reserved on " + dto.getStartDate() + " to " + dto.getEndDate() + ".");
                        model.addAttribute("pkg", existingPkg);
                        model.addAttribute("resources", resourceService.getActiveResources());
                        return "catalog/package-form";
                    }
                }
            }
        }

        // Step 5b: Validate and store uploaded package image if a new one is selected
        MultipartFile fileToUpload = (image != null && !image.isEmpty()) ? image : dto.getImage();
        String imageUrl = null;
        if (fileToUpload != null && !fileToUpload.isEmpty()) {
            try {
                imageUrl = packageService.validateAndStorePackageImage(fileToUpload);
            } catch (ValidationException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
                model.addAttribute("pkg", existingPkg);
                model.addAttribute("resources", resourceService.getActiveResources());
                return "catalog/package-form";
            }
        }

        // Step 6: Update package details in the database
        packageService.updatePackage(
                id,
                dto.getName(),
                dto.getDestination(),
                dto.getDescription(),
                dto.getBasePrice(),
                dto.getDurationDays(),
                imageUrl,
                existingPkg.getImageData(),
                existingPkg.getImageMimeType(),
                dto.getStatus()
        );

        // Step 7: Save new resource assignments for the package
        if (dto.getResourceIds() != null && !dto.getResourceIds().isEmpty()) {
            java.time.LocalDate start = (dto.getStartDate() != null) ? dto.getStartDate() : java.time.LocalDate.now().plusDays(7);
            java.time.LocalDate end = (dto.getEndDate() != null) ? dto.getEndDate() : start.plusDays(dto.getDurationDays() != null ? dto.getDurationDays() : 3);

            for (Long rId : dto.getResourceIds()) {
                com.islandtrails.resource.entity.ResourceAssignment ra = new com.islandtrails.resource.entity.ResourceAssignment(
                        id,
                        rId,
                        start,
                        end,
                        com.islandtrails.resource.entity.ResourceAssignmentStatus.RESERVED
                );
                resourceAssignmentRepository.save(ra);
            }
        }

        // Step 8: Set success message and redirect back to package list
        redirectAttributes.addFlashAttribute("successMessage", "Tour package and assigned resource schedules updated successfully.");
        return "redirect:/staff/tour-ops/packages";
    }

    // Updates the lifecycle status of a package
    @PostMapping("/staff/tour-ops/package/{id}/status")
    public String updatePackageStatus(@PathVariable("id") Long id,
                                      @RequestParam("status") PackageStatus status,
                                      RedirectAttributes redirectAttributes) {
        // Step 1: Update status through PackageService
        try {
            packageService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Package status updated to " + status.name());
        } catch (ValidationException ex) {
            // Step 3: Handle validation error if status update fails
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // Step 4: Redirect to package management dashboard
        return "redirect:/staff/tour-ops/packages";
    }

    // Handles deleting or archiving a package (Staff Tour Ops only)
    @PostMapping("/staff/tour-ops/package/{id}/delete")
    public String deletePackage(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        // Step 1: Attempt to delete or archive the package through PackageService
        try {
            boolean hardDeleted = packageService.deletePackage(id);
            if (hardDeleted) {
                redirectAttributes.addFlashAttribute("successMessage", "Tour package permanently deleted from database.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Tour package has booking history and was successfully archived. It has been removed from all catalog and active management views.");
            }
        } catch (ValidationException ex) {
            // Step 2: Show error message if validation fails
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // Step 3: Redirect to package management dashboard
        return "redirect:/staff/tour-ops/packages";
    }
}
