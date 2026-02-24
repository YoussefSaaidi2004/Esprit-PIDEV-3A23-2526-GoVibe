package org.example.entities;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Checkout} entity — constructors,
 * all getters/setters, and BigDecimal price handling.
 */
@DisplayName("Checkout entity — full coverage")
class CheckoutEntityTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. No-arg constructor
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("No-arg constructor: all fields null / zero")
    void noArgDefaults() {
        Checkout c = new Checkout();
        assertEquals(0,  c.getCheckoutId());
        assertNull(c.getFlightId());
        assertEquals(0,  c.getIdUser());
        assertNull(c.getReservationDate());
        assertEquals(0,  c.getPassengerNbr());
        assertNull(c.getStatusReservation());
        assertNull(c.getTotalPrix());
        assertNull(c.getPassengerName());
        assertNull(c.getPassengerEmail());
        assertNull(c.getPassengerPhone());
        assertNull(c.getTravelClass());
        assertNull(c.getSeatPreference());
        assertNull(c.getPaymentMethod());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. 6-arg constructor
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6-arg constructor")
    class SixArgConstructor {

        private Checkout c;
        private final LocalDateTime NOW = LocalDateTime.of(2026, 2, 24, 10, 0);

        @BeforeEach
        void setUp() {
            c = new Checkout(
                "TN101",
                42,
                NOW,
                2,
                "CONFIRMED",
                new BigDecimal("499.98")
            );
        }

        @Test void flightId()     { assertEquals("TN101", c.getFlightId()); }
        @Test void idUser()       { assertEquals(42, c.getIdUser()); }
        @Test void date()         { assertEquals(NOW, c.getReservationDate()); }
        @Test void paxNbr()       { assertEquals(2, c.getPassengerNbr()); }
        @Test void status()       { assertEquals("CONFIRMED", c.getStatusReservation()); }
        @Test void totalPrix()    { assertEquals(new BigDecimal("499.98"), c.getTotalPrix()); }

        @Test
        @DisplayName("checkoutId defaults to 0 when not explicitly set")
        void checkoutIdDefaultsZero() {
            assertEquals(0, c.getCheckoutId());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Setters round-trip
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Setters round-trip")
    class SettersRoundTrip {

        private final Checkout c = new Checkout();

        @Test void checkoutId()     { c.setCheckoutId(99);   assertEquals(99, c.getCheckoutId()); }
        @Test void flightId()       { c.setFlightId("AB1");  assertEquals("AB1", c.getFlightId()); }
        @Test void idUser()         { c.setIdUser(7);        assertEquals(7, c.getIdUser()); }
        @Test void passengerNbr()   { c.setPassengerNbr(5);  assertEquals(5, c.getPassengerNbr()); }
        @Test void status()         {
            c.setStatusReservation("PENDING");
            assertEquals("PENDING", c.getStatusReservation());
        }
        @Test void passengerName()  {
            c.setPassengerName("Ahmed Trabelsi");
            assertEquals("Ahmed Trabelsi", c.getPassengerName());
        }
        @Test void passengerEmail() {
            c.setPassengerEmail("ahmed@govibe.tn");
            assertEquals("ahmed@govibe.tn", c.getPassengerEmail());
        }
        @Test void passengerPhone() {
            c.setPassengerPhone("+21612345678");
            assertEquals("+21612345678", c.getPassengerPhone());
        }
        @Test void travelClass()    {
            c.setTravelClass("Business");
            assertEquals("Business", c.getTravelClass());
        }
        @Test void seatPreference() {
            c.setSeatPreference("Window");
            assertEquals("Window", c.getSeatPreference());
        }
        @Test void paymentMethod()  {
            c.setPaymentMethod("Stripe");
            assertEquals("Stripe", c.getPaymentMethod());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. BigDecimal totalPrix precision
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("totalPrix — BigDecimal precision")
    class TotalPrixPrecision {

        @Test
        @DisplayName("exact two-decimal-place value preserved")
        void twoDecimalPlaces() {
            Checkout c = new Checkout();
            c.setTotalPrix(new BigDecimal("123.45"));
            assertEquals(new BigDecimal("123.45"), c.getTotalPrix());
        }

        @Test
        @DisplayName("zero price preserved")
        void zeroPrice() {
            Checkout c = new Checkout();
            c.setTotalPrix(BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, c.getTotalPrix());
        }

        @Test
        @DisplayName("large price preserved (no float rounding)")
        void largePrice() {
            Checkout c = new Checkout();
            BigDecimal large = new BigDecimal("99999999.99");
            c.setTotalPrix(large);
            assertEquals(large, c.getTotalPrix());
        }

        @Test
        @DisplayName("null price accepted (no constraint in entity)")
        void nullPriceAccepted() {
            Checkout c = new Checkout();
            c.setTotalPrix(null);
            assertNull(c.getTotalPrix());
        }

        @ParameterizedTest(name = "price = {0}")
        @ValueSource(strings = {"0.00", "100.00", "299.99", "1000.50"})
        void variousPricesRoundTrip(String price) {
            Checkout c = new Checkout();
            BigDecimal bd = new BigDecimal(price);
            c.setTotalPrix(bd);
            assertEquals(bd, c.getTotalPrix());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Reservation date precision
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reservation date stored with nanosecond precision")
    void reservationDatePrecision() {
        LocalDateTime precise = LocalDateTime.of(2026, 6, 15, 14, 30, 45, 123456789);
        Checkout c = new Checkout();
        c.setReservationDate(precise);
        assertEquals(precise, c.getReservationDate());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Status values
    // ──────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "status = \"{0}\"")
    @ValueSource(strings = {"CONFIRMED", "PENDING", "CANCELLED", "REFUNDED"})
    @DisplayName("status values round-trip")
    void statusRoundTrip(String status) {
        Checkout c = new Checkout();
        c.setStatusReservation(status);
        assertEquals(status, c.getStatusReservation());
    }
}
