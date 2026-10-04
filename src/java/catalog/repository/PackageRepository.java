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

    List<Package> findByStatus(PackageStatus status);

    List<Package> findByStatusAndOrigin(PackageStatus status, PackageOrigin origin);

    @Query("SELECT p FROM Package p WHERE p.status = 'PUBLISHED' AND p.origin = 'BROWSABLE' " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
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

    List<Package> findByCreatedBy(Long createdBy);
}
