package com.islandtrails.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RefundRequestDTO {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "Reason for refund is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
