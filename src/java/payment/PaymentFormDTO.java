package com.islandtrails.payment.dto;

import com.islandtrails.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Form data transferred from the customer checkout page
public class PaymentFormDTO {

    @NotNull(message = "Required")
    private Long bookingId;

    @NotNull(message = "Required")
    private PaymentMethod method;

    @NotBlank(message = "Card or account number is required")
    @Pattern(regexp = "^[0-9]{4}[ -]?[0-9]{4}[ -]?[0-9]{4}[ -]?[0-9]{1,7}$", message = "Please enter a valid card number with 13 to 19 digits.")
    @Size(min = 12, max = 30, message = "12–30 characters")
    private String cardNumber;

    @NotBlank(message = "Expiry date is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])([/-]?)([0-9]{2}|[0-9]{4})$", message = "Expiry date must be in MM/YY, MM-YY, or MM/YYYY format (e.g. 12/28).")
    @Size(max = 10, message = "Max 10 characters")
    private String cardExpiry;

    @NotBlank(message = "CVV security code is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits.")
    @Size(min = 3, max = 4, message = "3 or 4 digits")
    private String cardCvv;

    // Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }

    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
}
