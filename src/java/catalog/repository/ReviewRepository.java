package com.islandtrails.catalog.repository;

import com.islandtrails.catalog.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPackageIdOrderByCreatedAtDesc(Long packageId);

    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Review> findByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.packageId = :packageId")
    Double calculateAverageRating(@Param("packageId") Long packageId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.packageId = :packageId")
    Long countReviewsByPackageId(@Param("packageId") Long packageId);
}
