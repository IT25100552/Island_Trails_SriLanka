package com.islandtrails.payment.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentGatewayService {

    public String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public boolean simulatePayment(String cardNumber) {
        if (cardNumber != null && cardNumber.endsWith("0000")) {
            return false; // Simulate decline for cards ending in 0000
        }
        return true;
    }
}
