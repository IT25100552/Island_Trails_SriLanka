package com.islandtrails.tripplanning.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TripRequestFormDTO {

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "1.00", message = "Budget must be greater than zero")
    private BigDecimal budget;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 255, message = "Interests cannot exceed 255 characters")
    private String interests;

    @Size(max = 2000, message = "Special requirements cannot exceed 2000 characters")
    private String specialRequirements;

    @NotNull(message = "Number of travelers is required")
    @Min(value = 1, message = "At least 1 traveler is required")
    @Max(value = 50, message = "Maximum 50 travelers allowed")
    private Integer numberOfTravelers = 1;

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public Integer getNumberOfTravelers() { return numberOfTravelers; }
    public void setNumberOfTravelers(Integer numberOfTravelers) { this.numberOfTravelers = numberOfTravelers; }
}
