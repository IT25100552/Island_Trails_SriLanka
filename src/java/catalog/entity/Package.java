package com.islandtrails.catalog.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "packages")
public class Package extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Lob
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "image_mime_type", length = 50)
    private String imageMimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PackageStatus status = PackageStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PackageOrigin origin = PackageOrigin.BROWSABLE;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Package() {
    }

    public Package(String name, String destination, String description, BigDecimal basePrice, Integer durationDays, PackageStatus status, PackageOrigin origin, Long createdBy) {
        this.name = name;
        this.destination = destination;
        this.description = description;
        this.basePrice = basePrice;
        this.durationDays = durationDays;
        this.status = status;
        this.origin = origin;
        this.createdBy = createdBy;
        if (status == PackageStatus.PUBLISHED) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageMimeType() {
        return imageMimeType;
    }

    public void setImageMimeType(String imageMimeType) {
        this.imageMimeType = imageMimeType;
    }

    public PackageStatus getStatus() {
        return status;
    }

    public void setStatus(PackageStatus status) {
        this.status = status;
    }

    public PackageOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(PackageOrigin origin) {
        this.origin = origin;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
