package com.islandtrails.booking.entity;

import com.islandtrails.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Represents a customer booking for a tour package
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "package_id", nullable = false)
    private Long packageId; // ALWAYS SET (quotations auto-convert to Package first)

    @Column(name = "package_name")
    private String packageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "number_of_travelers", nullable = false)
    private Integer numberOfTravelers = 1;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Default constructor for JPA
    public Booking() {
    }

    // Creates a new booking with initial PENDING_PAYMENT status
    public Booking(Long customerId, String customerName, Long packageId, String packageName, LocalDate startDate, LocalDate endDate, BigDecimal totalPrice, Integer numberOfTravelers, String specialRequests) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.packageId = packageId;
        this.packageName = packageName;
        this.bookingDate = LocalDate.now();
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.numberOfTravelers = numberOfTravelers != null ? numberOfTravelers : 1;
        this.specialRequests = specialRequests;
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    // Getters and Setters
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getNumberOfTravelers() {
        return numberOfTravelers;
    }

    public void setNumberOfTravelers(Integer numberOfTravelers) {
        this.numberOfTravelers = numberOfTravelers;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
