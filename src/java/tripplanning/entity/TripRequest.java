package com.islandtrails.tripplanning.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trip_requests")
public class TripRequest extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 255)
    private String interests; // comma-separated: adventure,culture,wildlife,etc

    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    @Column(name = "number_of_travelers")
    private Integer numberOfTravelers = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripRequestStatus status = TripRequestStatus.SUBMITTED;

    public TripRequest() {
    }

    public TripRequest(Long customerId, String customerName, BigDecimal budget, LocalDate startDate, LocalDate endDate, String interests, String specialRequirements, Integer numberOfTravelers) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.budget = budget;
        this.startDate = startDate;
        this.endDate = endDate;
        this.interests = interests;
        this.specialRequirements = specialRequirements;
        this.numberOfTravelers = numberOfTravelers != null ? numberOfTravelers : 1;
        this.status = TripRequestStatus.SUBMITTED;
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

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public Integer getNumberOfTravelers() {
        return numberOfTravelers;
    }

    public void setNumberOfTravelers(Integer numberOfTravelers) {
        this.numberOfTravelers = numberOfTravelers;
    }

    public TripRequestStatus getStatus() {
        return status;
    }

    public void setStatus(TripRequestStatus status) {
        this.status = status;
    }
}
