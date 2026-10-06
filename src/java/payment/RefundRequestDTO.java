package com.islandtrails.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Form data submitted by a customer when requesting a refund
public class RefundRequestDTO {

    @NotNull(message = "Required")
    private Long bookingId;

    @NotBlank(message = "Required")
    @Size(max = 500, message = "Max 500 characters")
    private String reason;

    // Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
