package com.islandtrails.payment.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

// Service that simulates an external payment gateway for card processing
@Service
public class PaymentGatewayService {

    // Generates a unique transaction reference ID starting with 'TXN-'
    public String generateTransactionId() {
        // Step 1: Combine the current timestamp and a random UUID to create a unique ID
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // Simulates card authorization: cards ending in '0000' are declined, all others approved
    public boolean simulatePayment(String cardNumber) {
        // Step 1: Decline test cards ending with '0000'
        if (cardNumber != null && cardNumber.endsWith("0000")) {
            return false;
        }
        // Step 2: Approve all other cards
        return true;
    }
}
