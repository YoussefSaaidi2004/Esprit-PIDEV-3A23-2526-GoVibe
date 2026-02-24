package org.example.entities;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Flight} entity — constructor variants,
 * all getters/setters, and computed seat availability.
 */
@DisplayName("Flight entity — constructors, getters, setters, edge cases")
class FlightEntityTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. No-arg constructor
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("No-arg constructor defaults")
    class NoArgConstructor {

        @Test
        @DisplayName("all fields are null / zero after no-arg construction")
        void defaultsAreNullOrZero() {
            Flight f = new Flight();
            assertNull(f.getFlightId());
            assertNull(f.getDepartureAirport());
            assertNull(f.getDestination());
            assertNull(f.getDepartureTime());
            assertNull(f.getArrivalTime());
            assertNull(f.getClasseChaise());
            assertNull(f.getAirline());
            assertEquals(0, f.getPrix());
            assertEquals(0, f.getAvailableSeats());
            assertEquals(0, f.getTotalSeats());
            assertNull(f.getDescription());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Full constructor
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Full constructor")
    class FullConstructor {

        private Flight flight;

        @BeforeEach
        void setUp() {
            flight = new Flight(
                "TN101",
                "TUN",
                "Paris",
                LocalTime.of(8, 30),
                LocalTime.of(12, 0),
                "Economy",
                "Tunisair",
                250,
                120,
                180,
                "Direct flight"
            );
        }

        @Test @DisplayName("flightId correctly assigned")
        void flightId() { assertEquals("TN101", flight.getFlightId()); }

        @Test @DisplayName("departureAirport correctly assigned")
        void departureAirport() { assertEquals("TUN", flight.getDepartureAirport()); }

        @Test @DisplayName("destination correctly assigned")
        void destination() { assertEquals("Paris", flight.getDestination()); }

        @Test @DisplayName("departureTime correctly assigned")
        void departureTime() { assertEquals(LocalTime.of(8, 30), flight.getDepartureTime()); }

        @Test @DisplayName("arrivalTime correctly assigned")
        void arrivalTime() { assertEquals(LocalTime.of(12, 0), flight.getArrivalTime()); }

        @Test @DisplayName("classeChaise correctly assigned")
        void classeChaise() { assertEquals("Economy", flight.getClasseChaise()); }

        @Test @DisplayName("airline correctly assigned")
        void airline() { assertEquals("Tunisair", flight.getAirline()); }

        @Test @DisplayName("prix correctly assigned")
        void prix() { assertEquals(250, flight.getPrix()); }

        @Test @DisplayName("availableSeats correctly assigned")
        void availableSeats() { assertEquals(120, flight.getAvailableSeats()); }

        @Test @DisplayName("totalSeats correctly assigned")
        void totalSeats() { assertEquals(180, flight.getTotalSeats()); }

        @Test @DisplayName("description correctly assigned")
        void description() { assertEquals("Direct flight", flight.getDescription()); }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. setters round-trip
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Setters round-trip")
    class SettersRoundTrip {

        private final Flight flight = new Flight();

        @Test @DisplayName("setFlightId / getFlightId")
        void flightIdRoundTrip() {
            flight.setFlightId("AB999");
            assertEquals("AB999", flight.getFlightId());
        }

        @Test @DisplayName("setDepartureAirport / getDepartureAirport")
        void departureAirportRoundTrip() {
            flight.setDepartureAirport("CDG");
            assertEquals("CDG", flight.getDepartureAirport());
        }

        @Test @DisplayName("setDestination / getDestination")
        void destinationRoundTrip() {
            flight.setDestination("London");
            assertEquals("London", flight.getDestination());
        }

        @Test @DisplayName("setPrix / getPrix — various prices")
        void prixRoundTrip() {
            flight.setPrix(0);
            assertEquals(0, flight.getPrix());
            flight.setPrix(9999);
            assertEquals(9999, flight.getPrix());
        }

        @Test @DisplayName("setAvailableSeats / getAvailableSeats")
        void availableSeatsRoundTrip() {
            flight.setAvailableSeats(42);
            assertEquals(42, flight.getAvailableSeats());
        }

        @Test @DisplayName("setTotalSeats / getTotalSeats")
        void totalSeatsRoundTrip() {
            flight.setTotalSeats(200);
            assertEquals(200, flight.getTotalSeats());
        }

        @Test @DisplayName("setDepartureTime / getDepartureTime — midnight boundary")
        void departureTimeMidnight() {
            flight.setDepartureTime(LocalTime.MIDNIGHT);
            assertEquals(LocalTime.MIDNIGHT, flight.getDepartureTime());
        }

        @Test @DisplayName("setArrivalTime / getArrivalTime — end of day boundary")
        void arrivalTimeLate() {
            LocalTime t = LocalTime.of(23, 59);
            flight.setArrivalTime(t);
            assertEquals(t, flight.getArrivalTime());
        }

        @Test @DisplayName("setter accepts null flightId")
        void setFlightIdNull() {
            flight.setFlightId(null);
            assertNull(flight.getFlightId());
        }

        @Test @DisplayName("setDescription / getDescription")
        void descriptionRoundTrip() {
            flight.setDescription("Stopover in Rome");
            assertEquals("Stopover in Rome", flight.getDescription());
        }

        @Test @DisplayName("setAirline / getAirline")
        void airlineRoundTrip() {
            flight.setAirline("Lufthansa");
            assertEquals("Lufthansa", flight.getAirline());
        }

        @Test @DisplayName("setClasseChaise / getClasseChaise")
        void classeChaiseRoundTrip() {
            flight.setClasseChaise("Business");
            assertEquals("Business", flight.getClasseChaise());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Seat availability logic
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Seat availability edge cases")
    class SeatAvailability {

        @ParameterizedTest(name = "available={0}, total={1}")
        @CsvSource({
            "0,   180",   // fully booked
            "1,   180",   // one seat left
            "180, 180",   // completely empty
            "180, 180",   // all seats available
        })
        void availableSeatsCanBeSetIndependently(int available, int total) {
            Flight f = new Flight();
            f.setAvailableSeats(available);
            f.setTotalSeats(total);
            assertEquals(available, f.getAvailableSeats());
            assertEquals(total,     f.getTotalSeats());
        }

        @Test
        @DisplayName("available seats can exceed total (no constraint enforced in entity)")
        void availableCanExceedTotal() {
            // Entity is a simple data holder — validation is the service's responsibility
            Flight f = new Flight();
            f.setAvailableSeats(200);
            f.setTotalSeats(100);
            assertEquals(200, f.getAvailableSeats());
        }
    }
}
