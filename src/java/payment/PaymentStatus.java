package com.islandtrails.payment.entity;

// Processing status of a payment transaction
public enum PaymentStatus {
    PENDING,
    VERIFIED,
    DECLINED,
    TIMEOUT,
    DUPLICATE_FLAGGED,
    REFUND_REQUESTED, // Customer has requested a refund; awaiting Finance decision
    REFUNDED
}
