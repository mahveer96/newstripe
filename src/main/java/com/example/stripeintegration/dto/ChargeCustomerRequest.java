package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class ChargeCustomerRequest {
    private String customerId;
    private String paymentMethodId; // Optional: if null, Stripe will use default
    private Long amount; // in cents
    private String currency;
    private String description;
}
