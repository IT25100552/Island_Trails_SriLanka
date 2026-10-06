package com.islandtrails.catalog.repository;

import com.islandtrails.catalog.entity.Package;
import com.islandtrails.catalog.entity.PackageOrigin;
import com.islandtrails.catalog.entity.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    // Finds all non-archived packages
    List<Package> findByArchivedFalse();

    // Finds all packages by their status (e.g., PUBLISHED, DRAFT)
    List<Package> findByStatus(PackageStatus status);

    // Finds non-archived packages by status
    List<Package> findByStatusAndArchivedFalse(PackageStatus status);

    // Finds all packages by status and origin (e.g., published and browsable)
    List<Package> findByStatusAndOrigin(PackageStatus status, PackageOrigin origin);

    // Finds non-archived packages by status and origin
    List<Package> findByStatusAndOriginAndArchivedFalse(PackageStatus status, PackageOrigin origin);

    // Finds all published packages matching search keywords, destination, price, or duration
    @Query("SELECT p FROM Package p WHERE p.status = 'PUBLISHED' AND p.origin = 'BROWSABLE' AND p.archived = false " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(p.description AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:destination IS NULL OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
           "AND (:maxDuration IS NULL OR p.durationDays <= :maxDuration)")
    List<Package> searchPublishedPackages(
            @Param("keyword") String keyword,
            @Param("destination") String destination,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("maxDuration") Integer maxDuration
    );

    // Finds all packages created by a specific staff member
    List<Package> findByCreatedBy(Long createdBy);
}
