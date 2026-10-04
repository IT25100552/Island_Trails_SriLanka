package com.islandtrails.catalog.dto;

import com.islandtrails.catalog.entity.PackageStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class PackageFormDTO {

    private Long id;

    @NotBlank(message = "Package name is required")
    @Size(max = 150, message = "Package name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Destination is required")
    @Size(max = 100, message = "Destination cannot exceed 100 characters")
    private String destination;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be greater than zero")
    private BigDecimal basePrice;

    @NotNull(message = "Duration in days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Max(value = 60, message = "Duration cannot exceed 60 days")
    private Integer durationDays;

    private PackageStatus status = PackageStatus.DRAFT;
    private java.util.List<Long> resourceIds = new java.util.ArrayList<>();
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;

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
}
