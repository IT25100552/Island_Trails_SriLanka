package com.islandtrails.catalog.controller;

import com.islandtrails.auth.entity.User;
import com.islandtrails.auth.service.PermissionService;
import com.islandtrails.catalog.dto.PackageFormDTO;
import com.islandtrails.catalog.dto.PackageSearchDTO;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.entity.PackageOrigin;
import com.islandtrails.catalog.entity.PackageStatus;
import com.islandtrails.catalog.service.PackageService;
import com.islandtrails.catalog.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PackageController {

    private final PackageService packageService;
    private final ReviewService reviewService;
    private final PermissionService permissionService;
    private final com.islandtrails.resource.service.ResourceService resourceService;
    private final com.islandtrails.resource.service.ConflictDetectionService conflictDetectionService;
    private final com.islandtrails.resource.repository.ResourceAssignmentRepository resourceAssignmentRepository;

    public PackageController(PackageService packageService,
                             ReviewService reviewService,
                             PermissionService permissionService,
                             com.islandtrails.resource.service.ResourceService resourceService,
                             com.islandtrails.resource.service.ConflictDetectionService conflictDetectionService,
                             com.islandtrails.resource.repository.ResourceAssignmentRepository resourceAssignmentRepository) {
        this.packageService = packageService;
        this.reviewService = reviewService;
        this.permissionService = permissionService;
        this.resourceService = resourceService;
        this.conflictDetectionService = conflictDetectionService;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
    }

    @GetMapping("/catalog/search")
    public String searchPackages(@ModelAttribute("searchDTO") PackageSearchDTO searchDTO, Model model) {
        List<Package> results = packageService.searchPackages(
                searchDTO.getKeyword(),
                searchDTO.getDestination(),
                searchDTO.getMinPrice(),
                searchDTO.getMaxPrice(),
                searchDTO.getMaxDuration()
        );
        model.addAttribute("packages", results);
        model.addAttribute("searchDTO", searchDTO);
        return "catalog/search";
    }

    @GetMapping("/catalog/package/{id}")
    public String packageDetail(@PathVariable("id") Long id, Model model) {
        Package pkg = packageService.getPackageById(id);
        model.addAttribute("pkg", pkg);
        model.addAttribute("reviews", reviewService.getReviewsForPackage(id));
        model.addAttribute("avgRating", reviewService.getAverageRating(id));
        model.addAttribute("reviewCount", reviewService.getReviewCount(id));
        return "catalog/package-detail";
    }

    @GetMapping("/catalog/package/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> getPackageImage(@PathVariable("id") Long id) {
        Package pkg = packageService.getPackageById(id);
        if (pkg.getImageData() == null || pkg.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (pkg.getImageMimeType() != null && pkg.getImageMimeType().contains("png")) {
            mediaType = MediaType.IMAGE_PNG;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                .body(pkg.getImageData());
    }

    @GetMapping("/staff/tour-ops/packages")
    public String managePackages(Model model) {
        model.addAttribute("packages", packageService.getAllPackages());
        return "catalog/manage-packages";
    }

    @GetMapping("/staff/tour-ops/package/new")
    public String newPackageForm(Model model) {
        PackageFormDTO dto = new PackageFormDTO();
        model.addAttribute("packageDTO", dto);
        model.addAttribute("resources", resourceService.getActiveResources());
        return "catalog/package-form";
    }

    @PostMapping("/staff/tour-ops/package/new")
    public String createPackage(@Valid @ModelAttribute("packageDTO") PackageFormDTO dto,
                                BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resources", resourceService.getActiveResources());
            return "catalog/package-form";
        }

        User user = permissionService.getCurrentUser().orElseThrow();

        // Validate dates if assigned
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date cannot be before start date.");
                model.addAttribute("resources", resourceService.getActiveResources());
                return "catalog/package-form";
            }

            // Check conflicts on all selected resources
            if (dto.getResourceIds() != null) {
                for (Long rId : dto.getResourceIds()) {
                    if (conflictDetectionService.hasConflict(rId, dto.getStartDate(), dto.getEndDate())) {
                        com.islandtrails.resource.entity.Resource res = resourceService.getResourceById(rId);
                        model.addAttribute("errorMessage", "Resource Conflict: '" + res.getName() + "' is already reserved/confirmed on " + dto.getStartDate() + " to " + dto.getEndDate() + ". Double booking prevented.");
                        model.addAttribute("resources", resourceService.getActiveResources());
                        return "catalog/package-form";
                    }
                }
            }
        }

        Package savedPackage = packageService.createPackage(
                dto.getName(),
                dto.getDestination(),
                dto.getDescription(),
                dto.getBasePrice(),
                dto.getDurationDays(),
                null,
                null,
                PackageOrigin.BROWSABLE,
                user.getId(),
                dto.getStatus()
        );

        // Save resource assignments and block calendar if resources selected
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

        redirectAttributes.addFlashAttribute("successMessage", "Tour package created and assigned resources successfully reserved in the availability calendar.");
        return "redirect:/staff/tour-ops/packages";
    }

    @GetMapping("/staff/tour-ops/package/{id}/edit")
    public String editPackageForm(@PathVariable("id") Long id, Model model) {
        Package pkg = packageService.getPackageById(id);
        PackageFormDTO dto = new PackageFormDTO();
        dto.setId(pkg.getId());
        dto.setName(pkg.getName());
        dto.setDestination(pkg.getDestination());
        dto.setDescription(pkg.getDescription());
        dto.setBasePrice(pkg.getBasePrice());
        dto.setDurationDays(pkg.getDurationDays());
        dto.setStatus(pkg.getStatus());

        List<com.islandtrails.resource.entity.ResourceAssignment> assignments = resourceAssignmentRepository.findByPackageId(id);
        if (!assignments.isEmpty()) {
            List<Long> assignedResourceIds = assignments.stream().map(com.islandtrails.resource.entity.ResourceAssignment::getResourceId).toList();
            dto.setResourceIds(new java.util.ArrayList<>(assignedResourceIds));
            dto.setStartDate(assignments.get(0).getStartDate());
            dto.setEndDate(assignments.get(0).getEndDate());
        }

        model.addAttribute("packageDTO", dto);
        model.addAttribute("pkg", pkg);
        model.addAttribute("resources", resourceService.getActiveResources());
        return "catalog/package-form";
    }

    @PostMapping("/staff/tour-ops/package/{id}/edit")
    public String updatePackage(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("packageDTO") PackageFormDTO dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            Package pkg = packageService.getPackageById(id);
            model.addAttribute("pkg", pkg);
            model.addAttribute("resources", resourceService.getActiveResources());
            return "catalog/package-form";
        }

        Package existingPkg = packageService.getPackageById(id);

        // Validate dates if assigned
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date cannot be before start date.");
                model.addAttribute("pkg", existingPkg);
                model.addAttribute("resources", resourceService.getActiveResources());
                return "catalog/package-form";
            }

            // Remove old assignments for this package before conflict re-validation
            List<com.islandtrails.resource.entity.ResourceAssignment> oldAssignments = resourceAssignmentRepository.findByPackageId(id);
            for (com.islandtrails.resource.entity.ResourceAssignment old : oldAssignments) {
                if (old.getBookingId() == null) {
                    resourceAssignmentRepository.delete(old);
                }
            }

            if (dto.getResourceIds() != null) {
                for (Long rId : dto.getResourceIds()) {
                    if (conflictDetectionService.hasConflict(rId, dto.getStartDate(), dto.getEndDate())) {
                        com.islandtrails.resource.entity.Resource res = resourceService.getResourceById(rId);
                        model.addAttribute("errorMessage", "Resource Conflict: '" + res.getName() + "' is already reserved on " + dto.getStartDate() + " to " + dto.getEndDate() + ".");
                        model.addAttribute("pkg", existingPkg);
                        model.addAttribute("resources", resourceService.getActiveResources());
                        return "catalog/package-form";
                    }
                }
            }
        }

        packageService.updatePackage(
                id,
                dto.getName(),
                dto.getDestination(),
                dto.getDescription(),
                dto.getBasePrice(),
                dto.getDurationDays(),
                existingPkg.getImageData(),
                existingPkg.getImageMimeType(),
                dto.getStatus()
        );

        // Update resource assignments
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

        redirectAttributes.addFlashAttribute("successMessage", "Tour package and assigned resource schedules updated successfully.");
        return "redirect:/staff/tour-ops/packages";
    }

    @PostMapping("/staff/tour-ops/package/{id}/status")
    public String updatePackageStatus(@PathVariable("id") Long id,
                                      @RequestParam("status") PackageStatus status,
                                      RedirectAttributes redirectAttributes) {
        packageService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Package status updated to " + status.name());
        return "redirect:/staff/tour-ops/packages";
    }
}
