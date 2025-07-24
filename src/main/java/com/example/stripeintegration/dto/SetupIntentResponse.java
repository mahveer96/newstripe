package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class SetupIntentResponse {
    private String clientSecret;
    private String customerId; // Useful for frontend to know which customer is being used

    public SetupIntentResponse(String clientSecret, String customerId) {
        this.clientSecret = clientSecret;
        this.customerId = customerId;
    }
}
