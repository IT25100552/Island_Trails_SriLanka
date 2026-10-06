package com.islandtrails.resource.service;

import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.entity.ResourceType;
import com.islandtrails.resource.repository.ResourceAssignmentRepository;
import com.islandtrails.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentRepository resourceAssignmentRepository;

    // Injects repositories for resources and resource assignments
    public ResourceService(ResourceRepository resourceRepository, ResourceAssignmentRepository resourceAssignmentRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceAssignmentRepository = resourceAssignmentRepository;
    }

    // Returns all non-archived resources in the database
    public List<Resource> getAllResources() {
        return resourceRepository.findByArchivedFalse();
    }

    // Returns only non-archived active resources
    public List<Resource> getActiveResources() {
        return resourceRepository.findByStatusAndArchivedFalse(ResourceStatus.ACTIVE);
    }

    // Returns only non-archived inactive resources
    public List<Resource> getInactiveResources() {
        return resourceRepository.findByStatusAndArchivedFalse(ResourceStatus.INACTIVE);
    }

    // Returns non-archived resources matching a given status (ACTIVE or INACTIVE)
    public List<Resource> getResourcesByStatus(ResourceStatus status) {
        return resourceRepository.findByStatusAndArchivedFalse(status);
    }

    // Returns non-archived active resources of a specific type (e.g. VEHICLE, GUIDE)
    public List<Resource> getResourcesByType(ResourceType type) {
        return resourceRepository.findByResourceTypeAndStatusAndArchivedFalse(type, ResourceStatus.ACTIVE);
    }

    // Returns all non-archived resources of a specific type regardless of status
    public List<Resource> getAllResourcesByType(ResourceType type) {
        return resourceRepository.findByResourceTypeAndArchivedFalse(type);
    }

    // Looks up a resource by ID, wrapped in an Optional
    public Optional<Resource> findById(Long id) {
        return resourceRepository.findById(id);
    }

    // Gets a resource by ID or throws an error if not found
    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    // Re-activates an inactive resource so it can be booked again
    @Transactional
    public void reactivateResource(Long id) {
        // Step 1: Find the resource
        Resource resource = getResourceById(id);

        // Step 2: Mark it ACTIVE and save
        resource.setStatus(ResourceStatus.ACTIVE);
        resourceRepository.save(resource);
    }

    // Validates and saves a new resource
    @Transactional
    public Resource createResource(Resource resource) {
        // Step 1: Check required fields (name, cost, type)
        if (resource.getName() == null || resource.getName().isBlank()) {
            throw new ValidationException("Resource name cannot be empty.");
        }
        if (resource.getUnitCost() == null || resource.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit cost must be zero or greater.");
        }
        if (resource.getResourceType() == null) {
            throw new ValidationException("Resource type must be specified.");
        }

        // Step 2: Clear fields that do not apply to this resource type
        sanitizeFieldsByResourceType(resource);

        // Step 3: Save and return the new resource
        return resourceRepository.save(resource);
    }

    // Updates details of an existing resource
    @Transactional
    public Resource updateResource(Long id, Resource updated) {
        // Step 1: Validate required fields
        if (updated.getName() == null || updated.getName().isBlank()) {
            throw new ValidationException("Resource name cannot be empty.");
        }
        if (updated.getUnitCost() == null || updated.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit cost must be zero or greater.");
        }
        if (updated.getResourceType() == null) {
            throw new ValidationException("Resource type must be specified.");
        }

        // Step 2: Clean up type-specific fields
        sanitizeFieldsByResourceType(updated);

        // Step 3: Find the existing resource
        Resource existing = getResourceById(id);

        // Step 4: Update common fields
        existing.setName(updated.getName());
        existing.setResourceType(updated.getResourceType());
        existing.setUnitCost(updated.getUnitCost());
        existing.setStatus(updated.getStatus());

        // Step 5: Update vehicle-specific fields
        existing.setPlateNumber(updated.getPlateNumber());
        existing.setSeatCapacity(updated.getSeatCapacity());
        existing.setVehicleModel(updated.getVehicleModel());

        // Step 6: Update guide-specific fields
        existing.setLanguages(updated.getLanguages());
        existing.setCertification(updated.getCertification());
        existing.setContactNumber(updated.getContactNumber());

        // Step 7: Update hotel room-specific fields
        existing.setHotelName(updated.getHotelName());
        existing.setRoomNumber(updated.getRoomNumber());
        existing.setBedCapacity(updated.getBedCapacity());

        // Step 8: Save and return the updated resource
        return resourceRepository.save(existing);
    }

    // Soft-deletes a resource by marking it INACTIVE
    @Transactional
    public void deleteResource(Long id) {
        // Step 1: Find the resource
        Resource resource = getResourceById(id);

        // Step 2: Change status to INACTIVE and save
        resource.setStatus(ResourceStatus.INACTIVE);
        resourceRepository.save(resource);
    }

    // Deletes a resource: permanently hard-deletes if unassigned; soft-deletes (archives) if assignment history exists
    @Transactional
    public boolean hardDeleteResource(Long id) {
        // Step 1: Find the resource
        Resource resource = getResourceById(id);

        // Step 2: If resource is assigned to any tours or packages, archive it
        if (resourceAssignmentRepository.existsByResourceId(id)) {
            resource.setArchived(true);
            resource.setArchivedAt(LocalDateTime.now());
            resource.setStatus(ResourceStatus.INACTIVE);
            resourceRepository.save(resource);
            return false;
        }

        // Step 3: Permanently remove the unassigned resource from the database
        resourceRepository.delete(resource);
        return true;
    }

    // Clears fields that do not apply to the current resource type
    private void sanitizeFieldsByResourceType(Resource resource) {
        // Step 1: Return if resource type is null
        if (resource.getResourceType() == null) return;

        // Step 2: Null out fields that do not belong to this resource type
        switch (resource.getResourceType()) {
            case VEHICLE -> {
                resource.setLanguages(null);
                resource.setCertification(null);
                resource.setContactNumber(null);
                resource.setHotelName(null);
                resource.setRoomNumber(null);
                resource.setBedCapacity(null);
            }
            case GUIDE -> {
                resource.setPlateNumber(null);
                resource.setSeatCapacity(null);
                resource.setVehicleModel(null);
                resource.setHotelName(null);
                resource.setRoomNumber(null);
                resource.setBedCapacity(null);
            }
            case HOTEL_ROOM -> {
                resource.setPlateNumber(null);
                resource.setSeatCapacity(null);
                resource.setVehicleModel(null);
                resource.setLanguages(null);
                resource.setCertification(null);
                resource.setContactNumber(null);
            }
            case ACTIVITY -> {
                resource.setPlateNumber(null);
                resource.setSeatCapacity(null);
                resource.setVehicleModel(null);
                resource.setLanguages(null);
                resource.setCertification(null);
                resource.setContactNumber(null);
                resource.setHotelName(null);
                resource.setRoomNumber(null);
                resource.setBedCapacity(null);
            }
        }
    }

    // Returns active and non-archived resources assigned to a package (unlinked to specific bookings) for public display
    public List<Resource> getPublicResourcesForPackage(Long packageId) {
        if (packageId == null) {
            return java.util.Collections.emptyList();
        }
        List<com.islandtrails.resource.entity.ResourceAssignment> assignments = resourceAssignmentRepository.findByPackageIdAndBookingIdIsNull(packageId);
        if (assignments == null || assignments.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return assignments.stream()
                .map(com.islandtrails.resource.entity.ResourceAssignment::getResourceId)
                .distinct()
                .map(resourceRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(r -> r.getStatus() == ResourceStatus.ACTIVE && !r.isArchived())
                .sorted(java.util.Comparator.comparing(Resource::getResourceType).thenComparing(Resource::getName))
                .toList();
    }
}
