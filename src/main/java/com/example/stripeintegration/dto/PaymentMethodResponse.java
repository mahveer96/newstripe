package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class PaymentMethodResponse {
    private String id;
    private String type; // e.g., "card"
    private String brand; // e.g., "visa", "mastercard"
    private String last4; // last 4 digits of card
    private Long expMonth;
    private Long expYear;

    public PaymentMethodResponse(String id, String type, String brand, String last4, Long expMonth, Long expYear) {
        this.id = id;
        this.type = type;
        this.brand = brand;
        this.last4 = last4;
        this.expMonth = expMonth;
        this.expYear = expYear;
    }
}