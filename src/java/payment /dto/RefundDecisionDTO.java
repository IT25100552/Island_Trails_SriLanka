package com.islandtrails.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefundDecisionDTO {

    @NotBlank(message = "Decision (APPROVE/REJECT) is required")
    private String decision; // APPROVE or REJECT

    @Size(max = 500, message = "Decision reason cannot exceed 500 characters")
    private String decisionReason;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
}
