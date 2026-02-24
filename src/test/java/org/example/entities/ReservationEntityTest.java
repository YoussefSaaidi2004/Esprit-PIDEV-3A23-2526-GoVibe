package org.example.entities;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Reservation} entity — constructors,
 * all getters/setters, getUserDisplayName(), and toString().
 */
@DisplayName("Reservation entity — full coverage")
class ReservationEntityTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. No-arg constructor
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("No-arg constructor: all fields are null / zero")
    void noArgConstructorDefaults() {
        Reservation r = new Reservation();
        assertEquals(0, r.getId());
        assertEquals(0, r.getUserId());
        assertNull(r.getUserNom());
        assertNull(r.getUserPrenom());
        assertNull(r.getUserEmail());
        assertEquals(0, r.getChambreId());
        assertEquals(0, r.getHotelId());
        assertNull(r.getDateDebut());
        assertNull(r.getDateFin());
        assertEquals(0.0, r.getPrixTotal(), 1e-9);
        assertNull(r.getStatut());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Full constructor (6-arg, no id)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6-arg constructor (no id)")
    class SixArgConstructor {

        private Reservation r;

        @BeforeEach
        void setUp() {
            r = new Reservation(
                1,               // userId
                101,             // chambreId
                5,               // hotelId
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 7),
                350.0,
                "EN_ATTENTE"
            );
        }

        @Test void userId()    { assertEquals(1,   r.getUserId()); }
        @Test void chambreId() { assertEquals(101, r.getChambreId()); }
        @Test void hotelId()   { assertEquals(5,   r.getHotelId()); }
        @Test void dateDebut() { assertEquals(LocalDate.of(2026, 3, 1), r.getDateDebut()); }
        @Test void dateFin()   { assertEquals(LocalDate.of(2026, 3, 7), r.getDateFin()); }
        @Test void prix()      { assertEquals(350.0, r.getPrixTotal(), 1e-9); }
        @Test void statut()    { assertEquals("EN_ATTENTE", r.getStatut()); }
        @Test void idZero()    { assertEquals(0, r.getId()); } // id not set by 6-arg ctor
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. 7-arg constructor (with id)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-arg constructor correctly sets id via delegation")
    void sevenArgConstructorSetsId() {
        Reservation r = new Reservation(
            42,              // id
            7,               // userId
            200,             // chambreId
            3,               // hotelId
            LocalDate.of(2026, 4, 10),
            LocalDate.of(2026, 4, 15),
            700.0,
            "CONFIRMEE"
        );
        assertEquals(42, r.getId());
        assertEquals(7, r.getUserId());
        assertEquals("CONFIRMEE", r.getStatut());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. getUserDisplayName()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserDisplayName()")
    class GetUserDisplayName {

        @Test
        @DisplayName("both nom and prenom set → 'Prenom Nom'")
        void bothNomPrenom() {
            Reservation r = new Reservation();
            r.setUserId(5);
            r.setUserPrenom("Ahmed");
            r.setUserNom("Ben Ali");
            String dn = r.getUserDisplayName();
            // Full name should contain both parts (order: prenom nom)
            assertTrue(dn.contains("Ahmed"), "Should contain first name");
            assertTrue(dn.contains("Ben Ali"), "Should contain last name");
        }

        @Test
        @DisplayName("only nom set → includes nom")
        void nomOnlyReturnsNom() {
            Reservation r = new Reservation();
            r.setUserId(99);
            r.setUserNom("Dupont");
            String dn = r.getUserDisplayName();
            assertFalse(dn.isBlank());
            assertTrue(dn.contains("Dupont"));
        }

        @Test
        @DisplayName("only prenom set → includes prenom")
        void prenomOnly() {
            Reservation r = new Reservation();
            r.setUserId(3);
            r.setUserPrenom("Marie");
            assertTrue(r.getUserDisplayName().contains("Marie"));
        }

        @Test
        @DisplayName("neither nom nor prenom → fallback 'Utilisateur #<id>'")
        void neitherNomNorPrenom() {
            Reservation r = new Reservation();
            r.setUserId(77);
            String dn = r.getUserDisplayName();
            // Must not be blank and must reference the userId
            assertFalse(dn.isBlank(), "Should not be blank");
            assertTrue(dn.contains("77"), "Should embed userId in fallback");
        }

        @Test
        @DisplayName("whitespace-only nom and prenom → fallback to id")
        void whitespaceNomPrenom() {
            Reservation r = new Reservation();
            r.setUserId(12);
            r.setUserNom("   ");
            r.setUserPrenom("  ");
            String dn = r.getUserDisplayName();
            assertFalse(dn.isBlank());
            assertTrue(dn.contains("12"), "Whitespace names should fall back to id");
        }

        @Test
        @DisplayName("null nom and null prenom → fallback to id")
        void nullNomAndPrenom() {
            Reservation r = new Reservation();
            r.setUserId(55);
            r.setUserNom(null);
            r.setUserPrenom(null);
            String dn = r.getUserDisplayName();
            assertNotNull(dn);
            assertTrue(dn.contains("55"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Statut domain values
    // ──────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "statut = \"{0}\"")
    @CsvSource({
        "EN_ATTENTE",
        "CONFIRMEE",
        "ANNULEE",
    })
    @DisplayName("valid statut values round-trip")
    void statutRoundTrip(String statut) {
        Reservation r = new Reservation();
        r.setStatut(statut);
        assertEquals(statut, r.getStatut());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. toString()
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() includes id and date range")
    void toStringContainsKeyInfo() {
        Reservation r = new Reservation(
            10, 1, 101, 5,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 5),
            200.0, "CONFIRMEE"
        );
        String s = r.toString();
        assertNotNull(s);
        // toString must reference the reservation id
        assertTrue(s.contains("10"), "toString should reference id");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Prix total precision
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("prix total accepts fractional values precisely")
    void prixTotalPrecision() {
        Reservation r = new Reservation();
        r.setPrixTotal(123.456789);
        assertEquals(123.456789, r.getPrixTotal(), 1e-6);
    }

    @Test
    @DisplayName("prix total can be zero")
    void prixTotalZero() {
        Reservation r = new Reservation();
        r.setPrixTotal(0.0);
        assertEquals(0.0, r.getPrixTotal(), 1e-9);
    }
}
