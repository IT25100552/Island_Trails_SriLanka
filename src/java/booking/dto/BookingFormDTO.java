package com.islandtrails.booking.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Data transfer object for the customer booking form
public class BookingFormDTO {

    @NotNull(message = "Required")
    private Long packageId;

    @NotNull(message = "Required")
    @FutureOrPresent(message = "Must be today or later")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "Required")
    @Min(value = 1, message = "Min 1")
    @Max(value = 50, message = "Max 50")
    private Integer numberOfTravelers = 1;

    @Size(max = 1000, message = "Max 1,000 characters")
    private String specialRequests;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getNumberOfTravelers() { return numberOfTravelers; }
    public void setNumberOfTravelers(Integer numberOfTravelers) { this.numberOfTravelers = numberOfTravelers; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
}
