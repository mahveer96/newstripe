package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class PaymentConfirmation {
    private String paymentIntentId;
}
