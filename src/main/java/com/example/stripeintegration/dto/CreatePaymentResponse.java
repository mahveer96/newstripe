package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class CreatePaymentResponse {

    private String clientSecret;

    public CreatePaymentResponse(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
}
