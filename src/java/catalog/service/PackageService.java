package com.islandtrails.catalog.service;

import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.entity.PackageOrigin;
import com.islandtrails.catalog.entity.PackageStatus;
import com.islandtrails.catalog.repository.PackageRepository;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PackageService {

    private final PackageRepository packageRepository;

    public PackageService(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public List<Package> getAllPackages() {
        return packageRepository.findAll();
    }

    public List<Package> getPublishedPackages() {
        return packageRepository.findByStatusAndOrigin(PackageStatus.PUBLISHED, PackageOrigin.BROWSABLE);
    }

    public List<Package> searchPackages(String keyword, String destination, BigDecimal minPrice, BigDecimal maxPrice, Integer maxDuration) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanDest = (destination != null && !destination.trim().isEmpty()) ? destination.trim() : null;

        return packageRepository.searchPublishedPackages(cleanKeyword, cleanDest, minPrice, maxPrice, maxDuration);
    }

    public Optional<Package> findById(Long id) {
        return packageRepository.findById(id);
    }

    public Package getPackageById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package not found with id: " + id));
    }

    @Transactional
    public Package createPackage(String name, String destination, String description, BigDecimal basePrice, Integer durationDays, byte[] imageData, String imageMimeType, PackageOrigin origin, Long createdBy, PackageStatus status) {
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

        Package pkg = new Package(name, destination, description, basePrice, durationDays, status != null ? status : PackageStatus.DRAFT, origin != null ? origin : PackageOrigin.BROWSABLE, createdBy);
        if (imageData != null && imageData.length > 0) {
            pkg.setImageData(imageData);
            pkg.setImageMimeType(imageMimeType);
        }

        return packageRepository.save(pkg);
    }

    @Transactional
    public Package updatePackage(Long id, String name, String destination, String description, BigDecimal basePrice, Integer durationDays, byte[] imageData, String imageMimeType, PackageStatus status) {
        Package pkg = getPackageById(id);

        if (name != null && !name.isBlank()) pkg.setName(name);
        if (destination != null && !destination.isBlank()) pkg.setDestination(destination);
        if (description != null) pkg.setDescription(description);
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) pkg.setBasePrice(basePrice);
        if (durationDays != null && durationDays > 0) pkg.setDurationDays(durationDays);
        if (imageData != null && imageData.length > 0) {
            pkg.setImageData(imageData);
            pkg.setImageMimeType(imageMimeType);
        }
        if (status != null) {
            if (pkg.getStatus() != PackageStatus.PUBLISHED && status == PackageStatus.PUBLISHED) {
                pkg.setPublishedAt(LocalDateTime.now());
            }
            pkg.setStatus(status);
        }

        return packageRepository.save(pkg);
    }

    @Transactional
    public Package updateStatus(Long id, PackageStatus status) {
        Package pkg = getPackageById(id);
        if (pkg.getStatus() != PackageStatus.PUBLISHED && status == PackageStatus.PUBLISHED) {
            pkg.setPublishedAt(LocalDateTime.now());
        }
        pkg.setStatus(status);
        return packageRepository.save(pkg);
    }

    @Transactional
    public void deleteOrDeactivate(Long id) {
        Package pkg = getPackageById(id);
        pkg.setStatus(PackageStatus.INACTIVE);
        packageRepository.save(pkg);
    }
}
