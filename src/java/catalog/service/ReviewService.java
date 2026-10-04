package com.islandtrails.catalog.service;

import com.islandtrails.catalog.entity.Review;
import com.islandtrails.catalog.repository.ReviewRepository;
import com.islandtrails.common.exception.ResourceNotFoundException;
import com.islandtrails.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> getReviewsForPackage(Long packageId) {
        return reviewRepository.findByPackageIdOrderByCreatedAtDesc(packageId);
    }

    public List<Review> getReviewsByCustomer(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Double getAverageRating(Long packageId) {
        Double avg = reviewRepository.calculateAverageRating(packageId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    public Long getReviewCount(Long packageId) {
        Long count = reviewRepository.countReviewsByPackageId(packageId);
        return count != null ? count : 0L;
    }

    @Transactional
    public Review addReview(Long bookingId, Long customerId, String customerName, Long packageId, Integer rating, String comment, byte[] photoData, String photoMimeType) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5 stars.");
        }
        if (comment == null || comment.isBlank()) {
            throw new ValidationException("Review comment cannot be empty.");
        }

        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new ValidationException("A review has already been submitted for this booking.");
        }

        Review review = new Review(bookingId, customerId, customerName, packageId, rating, comment.trim());
        if (photoData != null && photoData.length > 0) {
            review.setPhotoData(photoData);
            review.setPhotoMimeType(photoMimeType);
        }

        return reviewRepository.save(review);
    }

    @Transactional
    public Review respondToReview(Long reviewId, String staffResponse) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (staffResponse == null || staffResponse.isBlank()) {
            throw new ValidationException("Staff response cannot be empty.");
        }

        review.setStaffResponse(staffResponse.trim());
        review.setRespondedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }
}
