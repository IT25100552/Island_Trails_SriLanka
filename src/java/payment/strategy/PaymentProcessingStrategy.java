package com.islandtrails.payment.strategy;

import com.islandtrails.payment.entity.PaymentStatus;
import java.math.BigDecimal;

public interface PaymentProcessingStrategy {
    PaymentStatus process(BigDecimal amount, String cardDetails);
}
