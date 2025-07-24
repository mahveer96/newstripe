package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class CreatePayment {
    private Long amount; // Amount in cents (e.g., $10.00 -> 1000)
    private String currency; // e.g., "usd"
    private String description; // Optional
	
}

