package com.example.stripeintegration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.stripe.Stripe;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;


@Configuration
public class StripeConfig {

    private String secretKey;
@PostConstruct
public void init() {
    Dotenv dotenv = Dotenv.load();
    secretKey = dotenv.get("stripe.secret-key");
    Stripe.apiKey = secretKey;
    ///System.out.println("Loaded Stripe Key: " + secretKey);
}

// getter
public String getSecretKey() {
    return secretKey;
}




    /*  @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }*/
    
}
