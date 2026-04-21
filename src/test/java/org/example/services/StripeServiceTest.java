package org.example.services;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class StripeServiceTest {

    @Test
    void testCreateCheckoutSession() throws StripeException {
        StripeService stripeService = StripeService.getInstance();
        
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            // Setup mock behavior
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);
            mockedSession.when(mockSession::getUrl).thenReturn("https://checkout.stripe.com/test");

            String url = stripeService.createCheckoutSession(
                    123,
                    new BigDecimal("100.00"),
                    "Test Flight",
                    "http://success",
                    "http://cancel"
            );

            assertEquals("https://checkout.stripe.com/test", url);
        }
    }
}
