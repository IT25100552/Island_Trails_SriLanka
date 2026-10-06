package com.islandtrails.catalog.dto;

import com.islandtrails.catalog.entity.PackageStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// Form data for creating or editing a tour package
public class PackageFormDTO {

    private Long id;

    @NotBlank(message = "Required")
    @Size(max = 150, message = "Max 150 characters")
    private String name;

    @NotBlank(message = "Required")
    @Size(max = 100, message = "Max 100 characters")
    private String destination;

    @NotBlank(message = "Required")
    private String description;

    @NotNull(message = "Required")
    @Positive(message = "Must be > 0")
    private BigDecimal basePrice;

    @NotNull(message = "Required")
    @Min(value = 1, message = "Min 1 day")
    @Max(value = 60, message = "Max 60 days")
    private Integer durationDays;

    private String imageUrl;
    private org.springframework.web.multipart.MultipartFile image;

    private PackageStatus status = PackageStatus.PUBLISHED;
    private java.util.List<Long> resourceIds = new java.util.ArrayList<>();
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;

    // Getters and Setters
    public java.util.List<Long> getResourceIds() { return resourceIds; }
    public void setResourceIds(java.util.List<Long> resourceIds) { this.resourceIds = resourceIds; }

    public java.time.LocalDate getStartDate() { return startDate; }
    public void setStartDate(java.time.LocalDate startDate) { this.startDate = startDate; }

    public java.time.LocalDate getEndDate() { return endDate; }
    public void setEndDate(java.time.LocalDate endDate) { this.endDate = endDate; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public PackageStatus getStatus() { return status; }
    public void setStatus(PackageStatus status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public org.springframework.web.multipart.MultipartFile getImage() { return image; }
    public void setImage(org.springframework.web.multipart.MultipartFile image) { this.image = image; }
}
