package com.islandtrails.payment.strategy;

import com.islandtrails.payment.entity.PaymentStatus;

import java.math.BigDecimal;

public class PayLaterPaymentStrategy implements PaymentProcessingStrategy {
    @Override
    public PaymentStatus process(BigDecimal amount, String cardDetails) {
        return PaymentStatus.PENDING;
    }
}
