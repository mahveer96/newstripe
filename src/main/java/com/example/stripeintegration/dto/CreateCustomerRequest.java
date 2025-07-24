package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class CreateCustomerRequest {
    private String email;
    private String name;
    // You can add more fields like phone, address, etc.
}