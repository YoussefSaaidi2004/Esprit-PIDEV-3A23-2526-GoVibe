package org.example.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.math.BigDecimal;

public class StripeService {
    // In a real application, these should be securely stored
    private static final String SECRET_KEY =
            "sk_test_51T3li2Qm3Xy9KOw1AxtkDGxojEx7btUzVTXxsG5mreTjo96Lwl0VkOp0zAQyatIgfOf7hW0YsUqYGQi5705uj7ji00eiDOGnsG";

    private static StripeService instance;

    private StripeService() {
        // Use environment variable if available, else fallback to hardcoded key
        String envKey = System.getenv("STRIPE_SECRET_KEY");
        Stripe.apiKey = (envKey != null) ? envKey : SECRET_KEY;
    }

    public static StripeService getInstance() {
        if (instance == null) {
            instance = new StripeService();
        }
        return instance;
    }

    /**
     * Creates a Stripe Checkout Session
     * @param bookingId The ID of the booking to associate with this payment
     * @param amount The total amount to charge
     * @param description Brief description of what's being paid for
     * @param successUrl The URL to redirect to on successful payment
     * @param cancelUrl The URL to redirect to on payment cancellation
     * @return The URL of the Checkout Session
     * @throws StripeException if Stripe API call fails
     */
    public String createCheckoutSession(int bookingId, BigDecimal amount, String description, 
                                        String successUrl, String cancelUrl) throws StripeException {
        
        // Stripe expects amounts in cents (for USD/EUR) or millimes (for TND)
        // Here we'll treat it as standard cents logic for simplicity in demo
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?id=" + bookingId)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd") // Defaulting to USD for test compatibility
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(description)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

}
