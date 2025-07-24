package com.example.stripeintegration.dto;

import lombok.Data;

@Data
public class CustomerResponse {
    private String customerId;
    private String email;
    private String name;

    public CustomerResponse(String customerId, String email, String name) {
        this.customerId = customerId;
        this.email = email;
        this.name = name;
    }
}
