package com.example.stripeintegration.controller;

//package com.example.stripepayment.controller;

import com.example.stripeintegration.dto.*; // Import all DTOs
import com.example.stripeintegration.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.model.StripeObject;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:8080")
public class PaymentController {

    private final StripeService stripeService;

    @Value("${stripe.api.publishableKey}")
    private String stripePublishableKey;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    // --- Existing endpoints ---
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripePublishableKey);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<CreatePaymentResponse> createPaymentIntent(@RequestBody CreatePayment createPayment) {
        try {
            if (createPayment.getAmount() == null || createPayment.getAmount() <= 0) {
                return ResponseEntity.badRequest().body(new CreatePaymentResponse("Amount must be positive."));
            }
            if (createPayment.getCurrency() == null || createPayment.getCurrency().isEmpty()) {
                createPayment.setCurrency("usd");
            }

            PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                    createPayment.getAmount(),
                    createPayment.getCurrency(),
                    createPayment.getDescription());
            return ResponseEntity.ok(new CreatePaymentResponse(paymentIntent.getClientSecret()));

        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CreatePaymentResponse("Error creating PaymentIntent: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<Map<String, String>> confirmPayment(@RequestBody PaymentConfirmation confirmation) {
        try {
            PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(confirmation.getPaymentIntentId());

            if ("succeeded".equals(paymentIntent.getStatus())) {
                System.out.println("Backend Confirmed: PaymentIntent " + paymentIntent.getId() + " succeeded.");
                // YOUR APPLICATION LOGIC HERE: Update DB, send email, etc.
                return ResponseEntity.ok(Map.of("message", "Payment confirmed successfully!"));
            } else {
                System.out.println("Backend Confirmed: PaymentIntent " + paymentIntent.getId() + " status: "
                        + paymentIntent.getStatus());
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Payment not succeeded. Current status: " + paymentIntent.getStatus()));
            }

        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error confirming payment: " + e.getMessage()));
        }
    }

    // Webhook Endpoint (KEEP THIS - and make sure to add your secret!)
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader) {
        try {
            final String webhookSecret = "whsec_YOUR_WEBHOOK_SECRET"; // REPLACE WITH YOUR ACTUAL WEBHOOK SECRET

            com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("Received Stripe Webhook Event: " + event.getType());

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    Optional<StripeObject> piObj = event.getDataObjectDeserializer().getObject();
                    if (piObj.isPresent() && piObj.get() instanceof PaymentIntent) {
                        PaymentIntent paymentIntent = (PaymentIntent) piObj.get();
                        System.out.println("Webhook: PaymentIntent succeeded for ID: " + paymentIntent.getId()
                                + ". Amount: " + paymentIntent.getAmount());
                        // Update order status in DB to PAID
                    } else {
                        System.out.println("Webhook: Could not deserialize PaymentIntent object.");
                    }
                    break;
                case "payment_intent.payment_failed":
                    Optional<StripeObject> pfObj = event.getDataObjectDeserializer().getObject();
                    if (pfObj.isPresent() && pfObj.get() instanceof PaymentIntent) {
                        PaymentIntent paymentIntent = (PaymentIntent) pfObj.get();
                        System.out.println("Webhook: PaymentIntent failed for ID: " + paymentIntent.getId()
                                + ". Last error: " + paymentIntent.getLastPaymentError().getMessage());
                        // Update order status to FAILED, notify user
                    } else {
                        System.out.println("Webhook: Could not deserialize PaymentIntent object.");
                    }
                    break;
                case "setup_intent.succeeded":
                    Optional<StripeObject> siObj = event.getDataObjectDeserializer().getObject();
                    if (siObj.isPresent() && siObj.get() instanceof SetupIntent) {
                        SetupIntent setupIntent = (SetupIntent) siObj.get();
                        System.out.println("Webhook: SetupIntent succeeded for ID: " + setupIntent.getId()
                                + ". Customer: " + setupIntent.getCustomer());
                        // Payment method successfully saved. You might log this or update user profile.
                    } else {
                        System.out.println("Webhook: Could not deserialize SetupIntent object.");
                    }
                    break;
                default:
                    System.out.println("Webhook: Unhandled event type: " + event.getType());
            }
            return ResponseEntity.ok("Webhook received and processed.");
        } catch (com.stripe.exception.SignatureVerificationException e) {
            System.err.println("Error verifying webhook signature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature.");
        } catch (StripeException e) {
            System.err.println("Error processing Stripe event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event.");
        } catch (Exception e) {
            System.err.println("Generic error in webhook handler: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error.");
        }
    }

    // --- New Endpoints for Customer & Saved Cards ---

    /**
     * Endpoint to create a new Stripe Customer.
     * In a real app, this would be tied to your user registration.
     */
    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CreateCustomerRequest request) {
        try {
            Customer customer = stripeService.createStripeCustomer(request.getEmail(), request.getName());
            // In a real application, you would save customer.getId() to your user's
            // database record.
            System.out.println("Created Stripe Customer: " + customer.getId());
            return ResponseEntity.ok(new CustomerResponse(customer.getId(), customer.getEmail(), customer.getName()));
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CustomerResponse(null, null, "Error creating customer: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to get a SetupIntent client secret for saving a card.
     * Assumes you already have a customerId (e.g., stored in user session or DB).
     */
    @PostMapping("/customers/{customerId}/setup-intent")
    public ResponseEntity<SetupIntentResponse> createSetupIntent(@PathVariable String customerId) {
        try {
            // Verify if customerId exists in your DB first for security
            // If it doesn't exist, return 404 Not Found or 403 Forbidden

            SetupIntent setupIntent = stripeService.createSetupIntent(customerId);
            System.out.println("Created SetupIntent: " + setupIntent.getId() + " for customer " + customerId);
            return ResponseEntity.ok(new SetupIntentResponse(setupIntent.getClientSecret(), customerId));
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SetupIntentResponse("Error creating SetupIntent: " + e.getMessage(), customerId));
        }
    }

    /**
     * Endpoint to list all saved payment methods for a customer.
     * 
     * @param customerId The Stripe Customer ID.
     */
    @GetMapping("/customers/{customerId}/payment-methods")
    public ResponseEntity<List<PaymentMethodResponse>> getCustomerPaymentMethods(@PathVariable String customerId) {
        try {
            List<PaymentMethod> paymentMethods = stripeService.listCustomerPaymentMethods(customerId, "card"); // Only
                                                                                                               // list
                                                                                                               // cards
            List<PaymentMethodResponse> responses = paymentMethods.stream()
                    .map(pm -> new PaymentMethodResponse(
                            pm.getId(),
                            pm.getType(),
                            pm.getCard().getBrand(),
                            pm.getCard().getLast4(),
                            pm.getCard().getExpMonth(),
                            pm.getCard().getExpYear()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Or return an error DTO
        }
    }

    /**
     * Endpoint to remove a saved payment method from a customer.
     * 
     * @param paymentMethodId The ID of the PaymentMethod to detach.
     */
    @DeleteMapping("/payment-methods/{paymentMethodId}")
    public ResponseEntity<Map<String, String>> detachPaymentMethod(@PathVariable String paymentMethodId) {
        try {
            PaymentMethod detachedPm = stripeService.detachPaymentMethod(paymentMethodId);
            System.out.println("Detached PaymentMethod: " + detachedPm.getId());
            return ResponseEntity.ok(Map.of("message", "Payment method detached successfully."));
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error detaching payment method: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to charge an existing customer using one of their saved payment
     * methods.
     * Requires customerId and optionally paymentMethodId.
     */
    @PostMapping("/charge-customer")
    public ResponseEntity<Map<String, String>> chargeCustomer(@RequestBody ChargeCustomerRequest request) {
        try {
            // Validate input
            if (request.getCustomerId() == null || request.getCustomerId().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Customer ID is required."));
            }
            if (request.getAmount() == null || request.getAmount() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Amount must be positive."));
            }
            if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
                request.setCurrency("usd");
            }

            PaymentIntent paymentIntent = stripeService.chargeCustomerWithSavedCard(
                    request.getCustomerId(),
                    request.getPaymentMethodId(), // Can be null to use default
                    request.getAmount(),
                    request.getCurrency(),
                    request.getDescription());

            if ("succeeded".equals(paymentIntent.getStatus())) {
                System.out.println("Charged existing customer " + request.getCustomerId() + ". PaymentIntent: "
                        + paymentIntent.getId());
                // YOUR APPLICATION LOGIC: Update order status in DB to PAID
                return ResponseEntity
                        .ok(Map.of("message", "Payment successful!", "paymentIntentId", paymentIntent.getId()));
            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                // If the payment requires further action (e.g., 3D Secure), you'll need to send
                // the client_secret back to the frontend to handle the authentication.
                // For off-session charges, this is less common but can happen.
                return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                        "message", "Payment requires action.",
                        "paymentIntentId", paymentIntent.getId(),
                        "clientSecret", paymentIntent.getClientSecret()));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Payment not succeeded. Status: " + paymentIntent.getStatus()));
            }

        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error charging customer: " + e.getMessage()));
        }
    }
}