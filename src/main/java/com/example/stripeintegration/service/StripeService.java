package com.example.stripeintegration.service;
//
//package com.example.stripepayment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    /**
     * Creates a new PaymentIntent for a one-time charge.
     * @param amount The amount to charge (in the smallest currency unit, e.g., cents for USD).
     * @param currency The three-letter ISO currency code (e.g., "usd", "inr").
     * @param description An optional description for the payment.
     * @return The created PaymentIntent object.
     * @throws StripeException if the Stripe API call fails.
     */
    public PaymentIntent createPaymentIntent(Long amount, String currency, String description) throws StripeException {
        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .addPaymentMethodType("card"); // Allow card payments

        if (description != null && !description.isEmpty()) {
            paramsBuilder.setDescription(description);
        }

        PaymentIntentCreateParams params = paramsBuilder.build();
        return PaymentIntent.create(params);
    }

    /**
     * Retrieves an existing PaymentIntent from Stripe.
     * @param paymentIntentId The ID of the PaymentIntent to retrieve.
     * @return The PaymentIntent object.
     * @throws StripeException if the Stripe API call fails.
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Creates a new Customer object in Stripe.
     * @param email Customer's email.
     * @param name Customer's name.
     * @return The created Customer object.
     * @throws StripeException if the Stripe API call fails.
     */
    public Customer createStripeCustomer(String email, String name) throws StripeException {
        CustomerCreateParams.Builder paramsBuilder = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name);
        // You can add more parameters here like phone, address, metadata
        return Customer.create(paramsBuilder.build());
    }

    /**
     * Retrieves an existing Customer object from Stripe.
     * @param customerId Stripe Customer ID.
     * @return The Customer object.
     * @throws StripeException if the Stripe API call fails.
     */
    public Customer retrieveStripeCustomer(String customerId) throws StripeException {
        return Customer.retrieve(customerId);
    }

    /**
     * Creates a SetupIntent to collect and save a new payment method for a customer.
     * @param customerId The Stripe Customer ID to associate the payment method with.
     * @return The created SetupIntent object.
     * @throws StripeException if the Stripe API call fails.
     */
    public SetupIntent createSetupIntent(String customerId) throws StripeException {
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card") // Specify the payment method type you want to save
                .build();
        return SetupIntent.create(params);
    }

    /**
     * Lists all payment methods attached to a customer.
     * @param customerId Stripe Customer ID.
     * @param type Type of payment method (e.g., "card"). Use null or empty string for all types.
     * @return A list of PaymentMethod objects.
     * @throws StripeException if the Stripe API call fails.
     */
    public List<PaymentMethod> listCustomerPaymentMethods(String customerId, String type) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        if (type != null && !type.isEmpty()) {
            params.put("type", type);
        }
        return PaymentMethod.list(params).getData();
    }

    /**
     * Detaches a PaymentMethod from a Customer, effectively unsaving it.
     * @param paymentMethodId ID of the PaymentMethod to detach.
     * @return The detached PaymentMethod object.
     * @throws StripeException if the Stripe API call fails.
     */
    public PaymentMethod detachPaymentMethod(String paymentMethodId) throws StripeException {
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        return paymentMethod.detach();
    }

    /**
     * Charges a customer using a saved payment method.
     * If paymentMethodId is null or empty, it attempts to find and use the customer's default payment method.
     * If no default is set, it falls back to the first available card for that customer.
     *
     * @param customerId The Stripe Customer ID.
     * @param paymentMethodId The ID of the saved PaymentMethod to use (optional, if null, a default/first available will be used).
     * @param amount The amount to charge (in the smallest currency unit).
     * @param currency The currency (e.g., "usd").
     * @param description Optional description for the payment.
     * @return The created and confirmed PaymentIntent object.
     * @throws StripeException if the Stripe API call fails or no payment method is found.
     */
    public PaymentIntent chargeCustomerWithSavedCard(String customerId, String paymentMethodId, Long amount, String currency, String description) throws StripeException {
        // If no paymentMethodId is provided, attempt to find the customer's default
        // or the first available card.
        String actualPaymentMethodId = paymentMethodId;
        if (actualPaymentMethodId == null || actualPaymentMethodId.isEmpty()) {
            Customer customer = Customer.retrieve(customerId);
            if (customer.getInvoiceSettings() != null && customer.getInvoiceSettings().getDefaultPaymentMethod() != null) {
                actualPaymentMethodId = customer.getInvoiceSettings().getDefaultPaymentMethod();
                System.out.println("Using customer's default payment method: " + actualPaymentMethodId);
            } else {
                // Fallback: Retrieve the first available card if no default is set
                List<PaymentMethod> pms = listCustomerPaymentMethods(customerId, "card");
                if (!pms.isEmpty()) {
                    actualPaymentMethodId = pms.get(0).getId();
                    System.out.println("No default set, using first available card: " + actualPaymentMethodId);
                } else {
                    throw new RuntimeException("No payment method found for customer " + customerId + " to charge. Please save a card first.");
                }
            }
        }

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setCustomer(customerId)
                .setPaymentMethod(actualPaymentMethodId) // <-- This is now guaranteed to be set
                .setConfirm(true); // Automatically confirm the payment using the provided method

        if (description != null && !description.isEmpty()) {
            paramsBuilder.setDescription(description);
        }

        // Set off_session to true if you are charging a customer without them actively being online
        // and having gone through an authentication flow for this specific charge.
        // Requires previous consent (e.g., checkbox "save card for future payments").
        paramsBuilder.setOffSession(true);

        return PaymentIntent.create(paramsBuilder.build());
    }
}