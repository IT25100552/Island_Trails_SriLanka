package com.islandtrails.payment.strategy;

import com.islandtrails.payment.entity.PaymentStatus;
import com.islandtrails.payment.service.PaymentGatewayService;

import java.math.BigDecimal;

public class CreditCardPaymentStrategy implements PaymentProcessingStrategy {
    private final PaymentGatewayService gatewayService;

    public CreditCardPaymentStrategy(PaymentGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Override
    public PaymentStatus process(BigDecimal amount, String cardDetails) {
        return gatewayService.simulatePayment(cardDetails) ? PaymentStatus.VERIFIED : PaymentStatus.DECLINED;
    }
}
