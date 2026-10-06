package com.islandtrails.resource.repository;

import com.islandtrails.resource.entity.Resource;
import com.islandtrails.resource.entity.ResourceStatus;
import com.islandtrails.resource.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // Finds all non-archived resources
    List<Resource> findByArchivedFalse();

    // Finds resources by their status (ACTIVE or INACTIVE)
    List<Resource> findByStatus(ResourceStatus status);

    // Finds non-archived resources by status
    List<Resource> findByStatusAndArchivedFalse(ResourceStatus status);

    // Finds resources by both type and status
    List<Resource> findByResourceTypeAndStatus(ResourceType resourceType, ResourceStatus status);

    // Finds non-archived resources by both type and status
    List<Resource> findByResourceTypeAndStatusAndArchivedFalse(ResourceType resourceType, ResourceStatus status);

    // Finds all resources belonging to a specific type
    List<Resource> findByResourceType(ResourceType resourceType);

    // Finds non-archived resources belonging to a specific type
    List<Resource> findByResourceTypeAndArchivedFalse(ResourceType resourceType);
}
