package com.example.stripeintegration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.stripeintegration.dto.CreatePayment;
import com.example.stripeintegration.dto.CreatePaymentResponse;
import com.example.stripeintegration.entity.User;
import com.example.stripeintegration.service.Userservice;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@RestController
@RequestMapping("/api/users")
public class Usercontroller {
    @Autowired
    private  Userservice userService;
    
    @PostMapping("/adduser")
    public User addUser(@RequestBody User user) {

        return userService.addUser(user);
        
    }
    
}
