package com.islandtrails.payment.strategy;

import com.islandtrails.payment.entity.PaymentMethod;
import com.islandtrails.payment.service.PaymentGatewayService;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessingStrategyFactory {
    private final PaymentGatewayService gatewayService;

    public PaymentProcessingStrategyFactory(PaymentGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    public PaymentProcessingStrategy getStrategy(PaymentMethod method) {
        if (method == PaymentMethod.PAY_LATER || method == PaymentMethod.BANK_TRANSFER) {
            return new PayLaterPaymentStrategy();
        }
        if (method == PaymentMethod.DEBIT_CARD) {
            return new DebitCardPaymentStrategy(gatewayService);
        }
        return new CreditCardPaymentStrategy(gatewayService);
    }
}
