package com.example.stripeintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class StripeintegrationApplication {

	private static final Logger logger = LoggerFactory.getLogger(StripeintegrationApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(StripeintegrationApplication.class, args);
		logger.info("Welcome to the Stripe integration application");
	}

}
