package com.islandtrails.catalog.service;

import com.islandtrails.booking.repository.BookingRepository;
import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.entity.PackageOrigin;
import com.islandtrails.catalog.entity.PackageStatus;
import com.islandtrails.catalog.repository.PackageRepository;
import com.islandtrails.support.repository.ReviewRepository;
import com.islandtrails.catalog.state.PackageState;
import com.islandtrails.catalog.state.PackageStateFactory;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Business logic service for managing tour packages
@Service
public class PackageService {

    private final PackageRepository packageRepository;
    private final BookingRepository bookingRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;
    private final ReviewRepository reviewRepository;

    // Constructor injecting the repositories needed for package operations
    public PackageService(PackageRepository packageRepository,
                          BookingRepository bookingRepository,
                          ResourceAssignmentRepository resourceAssignmentRepository,
                          ReviewRepository reviewRepository) {
        this.packageRepository = packageRepository;
        this.bookingRepository = bookingRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
        this.reviewRepository = reviewRepository;
    }

    // Returns a list of all tour packages from the database (excluding archived)
    public List<Package> getAllPackages() {
        // Step 1: Query and return all non-archived packages from the repository
        return packageRepository.findByArchivedFalse();
    }

    // Returns only packages that are published and available for customers to browse (excluding archived)
    public List<Package> getPublishedPackages() {
        // Step 1: Fetch packages with PUBLISHED status and BROWSABLE origin, excluding archived
        return packageRepository.findByStatusAndOriginAndArchivedFalse(PackageStatus.PUBLISHED, PackageOrigin.BROWSABLE);
    }

    // Searches published packages using keyword, destination, price range, and duration
    public List<Package> searchPackages(String keyword, String destination, BigDecimal minPrice, BigDecimal maxPrice, Integer maxDuration) {
        // Step 1: Clean and trim the keyword and destination filter inputs
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanDest = (destination != null && !destination.trim().isEmpty()) ? destination.trim() : null;

        // Step 2: Run the custom search query in the repository
        return packageRepository.searchPublishedPackages(cleanKeyword, cleanDest, minPrice, maxPrice, maxDuration);
    }

    // Finds a package by its ID wrapped in an Optional
    public Optional<Package> findById(Long id) {
        // Step 1: Look up package by ID
        return packageRepository.findById(id);
    }

    // Gets a package by ID or throws an exception if not found
    public Package getPackageById(Long id) {
        // Step 1: Check if the package exists in the database
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package not found with id: " + id));
    }

    // Validates an uploaded package image and stores it in uploads/packages directory
    public String validateAndStorePackageImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Max size check (5 MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ValidationException("Image file size must not exceed 5 MB.");
        }

        // 2. MIME type check
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/jpg")
                && !contentType.equalsIgnoreCase("image/png")
                && !contentType.equalsIgnoreCase("image/webp"))) {
            throw new ValidationException("Invalid image format. Allowed formats: JPG, JPEG, PNG, WEBP.");
        }

        // 3. Extension check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ValidationException("Invalid image file name.");
        }

        String safeFileName = Paths.get(originalFilename).getFileName().toString();
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == safeFileName.length() - 1) {
            throw new ValidationException("Image file must have an extension (.jpg, .jpeg, .png, .webp).");
        }

        String extension = safeFileName.substring(dotIndex + 1).toLowerCase();
        if (!extension.matches("^(jpg|jpeg|png|webp)$")) {
            throw new ValidationException("Invalid image format. Allowed extensions: .jpg, .jpeg, .png, .webp.");
        }

        // 4. Generate unique filename (UUID + extension)
        String uniqueFilename = UUID.randomUUID().toString() + "." + (extension.equals("jpeg") ? "jpg" : extension);

        // 5. Save to project uploads directory
        try {
            Path uploadDir = Paths.get("uploads", "packages").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            Path destination = uploadDir.resolve(uniqueFilename).normalize();
            if (!destination.startsWith(uploadDir)) {
                throw new ValidationException("Invalid upload destination path.");
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/packages/" + uniqueFilename;
        } catch (IOException ex) {
            throw new ValidationException("Failed to store uploaded package image: " + ex.getMessage());
        }
    }

    // Safely deletes an uploaded package image file from disk
    public void deletePackageImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        if (imageUrl.startsWith("/uploads/packages/")) {
            try {
                String filename = imageUrl.substring("/uploads/packages/".length());
                String safeFilename = Paths.get(filename).getFileName().toString();
                Path uploadDir = Paths.get("uploads", "packages").toAbsolutePath().normalize();
                Path filePath = uploadDir.resolve(safeFilename).normalize();

                if (filePath.startsWith(uploadDir)) {
                    Files.deleteIfExists(filePath);
                }
            } catch (Exception ignored) {
                // Non-critical cleanup failure, preserve database integrity
            }
        }
    }

    // Creates and saves a new tour package after checking that required fields are valid
    @Transactional
    public Package createPackage(String name, String destination, String description, BigDecimal basePrice, Integer durationDays, String imageUrl, byte[] imageData, String imageMimeType, PackageOrigin origin, Long createdBy, PackageStatus status) {
        // Step 1: Validate required fields like name, destination, price, and duration
        if (name == null || name.isBlank()) {
            throw new ValidationException("Package name cannot be empty.");
        }
        if (destination == null || destination.isBlank()) {
            throw new ValidationException("Destination cannot be empty.");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Base price must be greater than zero.");
        }
        if (durationDays == null || durationDays <= 0) {
            throw new ValidationException("Duration must be at least 1 day.");
        }

        // Step 2: Set the initial status (default to PUBLISHED if not specified)
        PackageStatus initialStatus = status != null ? status : PackageStatus.PUBLISHED;
        Package pkg = new Package(name, destination, description, basePrice, durationDays, initialStatus, origin != null ? origin : PackageOrigin.BROWSABLE, createdBy);

        // Step 3: Record published timestamp if the package is published
        if (initialStatus == PackageStatus.PUBLISHED) {
            pkg.setPublishedAt(LocalDateTime.now());
        }

        // Step 4: Attach promotional image URL or binary data if provided
        if (imageUrl != null && !imageUrl.isBlank()) {
            pkg.setImageUrl(imageUrl.trim());
        }
        if (imageData != null && imageData.length > 0) {
            pkg.setImageData(imageData);
            pkg.setImageMimeType(imageMimeType);
        }

        // Step 5: Save the package to the database and return it
        return packageRepository.save(pkg);
    }

    // Overload for backwards compatibility
    @Transactional
    public Package createPackage(String name, String destination, String description, BigDecimal basePrice, Integer durationDays, byte[] imageData, String imageMimeType, PackageOrigin origin, Long createdBy, PackageStatus status) {
        return createPackage(name, destination, description, basePrice, durationDays, null, imageData, imageMimeType, origin, createdBy, status);
    }

    // Updates package details
    @Transactional
    public Package updatePackage(Long id, String name, String destination, String description, BigDecimal basePrice, Integer durationDays, String imageUrl, byte[] imageData, String imageMimeType, PackageStatus status) {
        // Step 1: Check if the package exists in the database
        Package pkg = getPackageById(id);

        // Step 2: Update non-empty fields with the new values
        if (name != null && !name.isBlank()) pkg.setName(name);
        if (destination != null && !destination.isBlank()) pkg.setDestination(destination);
        if (description != null) pkg.setDescription(description);
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) pkg.setBasePrice(basePrice);
        if (durationDays != null && durationDays > 0) pkg.setDurationDays(durationDays);

        // Step 4: Update promotional image URL if a new one was uploaded
        if (imageUrl != null && !imageUrl.isBlank()) {
            String oldImageUrl = pkg.getImageUrl();
            if (oldImageUrl != null && !oldImageUrl.equals(imageUrl)) {
                deletePackageImageFile(oldImageUrl);
            }
            pkg.setImageUrl(imageUrl.trim());
        }

        if (imageData != null && imageData.length > 0) {
            pkg.setImageData(imageData);
            pkg.setImageMimeType(imageMimeType);
        }

        // Step 5: Update the status and set published timestamp if newly published
        if (status != null) {
            if (pkg.getStatus() != PackageStatus.PUBLISHED && status == PackageStatus.PUBLISHED) {
                pkg.setPublishedAt(LocalDateTime.now());
            }
            pkg.setStatus(status);
        }

        // Step 6: Save the updated package to the database
        return packageRepository.save(pkg);
    }

    // Overload for backwards compatibility
    @Transactional
    public Package updatePackage(Long id, String name, String destination, String description, BigDecimal basePrice, Integer durationDays, byte[] imageData, String imageMimeType, PackageStatus status) {
        return updatePackage(id, name, destination, description, basePrice, durationDays, null, imageData, imageMimeType, status);
    }

    // Publishes a tour package if its current state permits publication
    @Transactional
    public Package publishPackage(Long id) {
        // Step 1: Check if the package exists in the database
        Package pkg = getPackageById(id);

        // Step 2: Validate state transition using State Pattern
        PackageState state = PackageStateFactory.getState(pkg.getStatus());
        if (!state.canPublish()) {
            throw new ValidationException("Package is already published.");
        }

        // Step 3: Transition status to PUBLISHED and record timestamp
        pkg.setStatus(PackageStatus.PUBLISHED);
        pkg.setPublishedAt(LocalDateTime.now());

        // Step 4: Save and return the updated package
        return packageRepository.save(pkg);
    }

    // Deactivates a tour package if its current state permits deactivation
    @Transactional
    public Package deactivatePackage(Long id) {
        // Step 1: Check if the package exists in the database
        Package pkg = getPackageById(id);

        // Step 2: Validate state transition using State Pattern
        PackageState state = PackageStateFactory.getState(pkg.getStatus());
        if (!state.canDeactivate()) {
            throw new ValidationException("Package cannot be deactivated from its current state.");
        }

        // Step 3: Transition status to INACTIVE
        pkg.setStatus(PackageStatus.INACTIVE);

        // Step 4: Save and return the updated package
        return packageRepository.save(pkg);
    }

    // Updates only the status of a package
    @Transactional
    public Package updateStatus(Long id, PackageStatus status) {
        if (status == null) {
            return getPackageById(id);
        }

        // Delegate to state-managed lifecycle methods
        if (status == PackageStatus.PUBLISHED) {
            return publishPackage(id);
        } else if (status == PackageStatus.INACTIVE) {
            return deactivatePackage(id);
        }

        // Step 3: Check if the package exists in the database
        Package pkg = getPackageById(id);

        // Step 4: Save the new status to the database
        pkg.setStatus(status);
        return packageRepository.save(pkg);
    }

    // Deletes a tour package: hard-deletes if no bookings exist; soft-deletes (archives) if booking history exists
    @Transactional
    public boolean deletePackage(Long id) {
        // Step 1: Check if the package exists in the database
        Package pkg = getPackageById(id);

        // Step 2: If package has bookings, soft-delete (archive) it to maintain relational and audit integrity
        if (bookingRepository.countByPackageId(id) > 0) {
            pkg.setArchived(true);
            pkg.setArchivedAt(LocalDateTime.now());
            pkg.setStatus(PackageStatus.INACTIVE);
            packageRepository.save(pkg);
            return false;
        }

        // Step 3: Remove any assigned resources linked to this package
        resourceAssignmentRepository.deleteByPackageId(id);

        // Step 4: Delete all customer reviews linked to this package
        reviewRepository.deleteByPackageId(id);

        // Step 4b: Delete physical image file from disk if present
        deletePackageImageFile(pkg.getImageUrl());

        // Step 5: Permanently delete the package from the database
        packageRepository.delete(pkg);
        return true;
    }

    // Soft-deactivates a package by changing its status to INACTIVE
    @Transactional
    public void deleteOrDeactivate(Long id) {
        // Step 1: Delegate to state-managed deactivation method
        deactivatePackage(id);
    }
}
