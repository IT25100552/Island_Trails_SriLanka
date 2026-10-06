package com.islandtrails.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Form data submitted by Finance staff when approving or rejecting a refund
public class RefundDecisionDTO {

    @NotBlank(message = "Required")
    private String decision; // APPROVE or REJECT

    @Size(max = 500, message = "Max 500 characters")
    private String decisionReason;

    // Getters and Setters
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
}
