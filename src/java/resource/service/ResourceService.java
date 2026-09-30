package com.islandtrails.resource.service;

import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.entity.ResourceType;
import com.islandtrails.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> getActiveResources() {
        return resourceRepository.findByStatus(ResourceStatus.ACTIVE);
    }

    public List<Resource> getResourcesByType(ResourceType type) {
        return resourceRepository.findByResourceTypeAndStatus(type, ResourceStatus.ACTIVE);
    }

    public Optional<Resource> findById(Long id) {
        return resourceRepository.findById(id);
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    @Transactional
    public Resource createResource(Resource resource) {
        if (resource.getName() == null || resource.getName().isBlank()) {
            throw new ValidationException("Resource name cannot be empty.");
        }
        if (resource.getUnitCost() == null || resource.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit cost must be zero or greater.");
        }
        if (resource.getResourceType() == null) {
            throw new ValidationException("Resource type must be specified.");
        }
        return resourceRepository.save(resource);
    }

    @Transactional
    public Resource updateResource(Long id, Resource updated) {
        if (updated.getName() == null || updated.getName().isBlank()) {
            throw new ValidationException("Resource name cannot be empty.");
        }
        if (updated.getUnitCost() == null || updated.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit cost must be zero or greater.");
        }
        if (updated.getResourceType() == null) {
            throw new ValidationException("Resource type must be specified.");
        }

        Resource existing = getResourceById(id);

        existing.setName(updated.getName());
        existing.setResourceType(updated.getResourceType());
        existing.setUnitCost(updated.getUnitCost());
        existing.setStatus(updated.getStatus());

        // Vehicle fields
        existing.setPlateNumber(updated.getPlateNumber());
        existing.setSeatCapacity(updated.getSeatCapacity());
        existing.setVehicleModel(updated.getVehicleModel());

        // Guide fields
        existing.setLanguages(updated.getLanguages());
        existing.setCertification(updated.getCertification());
        existing.setContactNumber(updated.getContactNumber());

        // Hotel fields
        existing.setHotelName(updated.getHotelName());
        existing.setRoomNumber(updated.getRoomNumber());
        existing.setBedCapacity(updated.getBedCapacity());

        return resourceRepository.save(existing);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = getResourceById(id);
        resource.setStatus(ResourceStatus.INACTIVE);
        resourceRepository.save(resource);
    }
}
