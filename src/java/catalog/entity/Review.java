package com.islandtrails.catalog.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Lob
    @Column(name = "photo_data")
    private byte[] photoData;

    @Column(name = "photo_mime_type", length = 50)
    private String photoMimeType;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "staff_response", columnDefinition = "TEXT")
    private String staffResponse;

    public Review() {
    }

    public Review(Long bookingId, Long customerId, String customerName, Long packageId, Integer rating, String comment) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.packageId = packageId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    public String getPhotoMimeType() {
        return photoMimeType;
    }

    public void setPhotoMimeType(String photoMimeType) {
        this.photoMimeType = photoMimeType;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getStaffResponse() {
        return staffResponse;
    }

    public void setStaffResponse(String staffResponse) {
        this.staffResponse = staffResponse;
    }
}
