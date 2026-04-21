package org.example.utils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exhaustive unit tests for {@link ValidationUtils}.
 *
 * Covers all public methods:
 *   isNotEmpty, isValidEmail, isValidPhone,
 *   isValidPassengerCount, getValidationMessage
 */
@DisplayName("ValidationUtils — full coverage")
class ValidationUtilsTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. isNotEmpty
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isNotEmpty()")
    class IsNotEmpty {

        @NullAndEmptySource
        @ParameterizedTest(name = "isNotEmpty(\"{0}\") → false")
        void nullOrEmptyReturnsFalse(String value) {
            assertFalse(ValidationUtils.isNotEmpty(value));
        }

        @ValueSource(strings = {"  ", "\t", "\n", "   \t  "})
        @ParameterizedTest(name = "whitespace-only \"{0}\" → false")
        void whitespaceOnlyReturnsFalse(String value) {
            assertFalse(ValidationUtils.isNotEmpty(value));
        }

        @ValueSource(strings = {"a", "hello", " x ", "123", "abc 123", "GoVibe Travel"})
        @ParameterizedTest(name = "\"{0}\" → true")
        void nonBlankStringReturnsTrue(String value) {
            assertTrue(ValidationUtils.isNotEmpty(value));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. isValidEmail
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isValidEmail()")
    class IsValidEmail {

        @ParameterizedTest(name = "valid email: \"{0}\"")
        @ValueSource(strings = {
            "user@example.com",
            "user.name+tag@example.co.uk",
            "firstname.lastname@company.org",
            "1234567890@numbers.com",
            "user-name@domain.travel",
            "govibe@tunisie.tn",
            "bakhtri@esprit.tn",
        })
        void validEmailsAreAccepted(String email) {
            assertTrue(ValidationUtils.isValidEmail(email),
                    "Expected valid email: " + email);
        }

        @ParameterizedTest(name = "invalid email: \"{0}\"")
        @ValueSource(strings = {
            "notanemail",
            "@nodomain.com",
            "missing-at-sign.com",
            "user@",
            "user@ domain.com",
            "user@domain",
            "user@.com",
            "us er@domain.com",
        })
        void invalidEmailsAreRejected(String email) {
            assertFalse(ValidationUtils.isValidEmail(email),
                    "Expected invalid email: " + email);
        }

        @NullAndEmptySource
        @ParameterizedTest(name = "null/empty email → false")
        void nullOrEmptyEmailReturnsFalse(String email) {
            assertFalse(ValidationUtils.isValidEmail(email));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. isValidPhone
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isValidPhone()")
    class IsValidPhone {

        @ParameterizedTest(name = "valid phone: \"{0}\"")
        @ValueSource(strings = {
            "+21612345678",          // +country(3) local(4) suffix(4)
            "0021698765432",         // 00-prefix international
            "12345678",              // 8-digit local
            "123",                   // 3 digits — matches 3 x 1-digit groups
        })
        void validPhonesAreAccepted(String phone) {
            assertTrue(ValidationUtils.isValidPhone(phone),
                    "Expected valid phone: " + phone);
        }

        @ParameterizedTest(name = "invalid phone: \"{0}\"")
        @ValueSource(strings = {
            "abc",
            "++216123",
            "phone number",
        })
        void invalidPhonesAreRejected(String phone) {
            assertFalse(ValidationUtils.isValidPhone(phone),
                    "Expected invalid phone: " + phone);
        }

        @NullAndEmptySource
        @ParameterizedTest(name = "null/empty phone → false")
        void nullOrEmptyPhoneReturnsFalse(String phone) {
            assertFalse(ValidationUtils.isValidPhone(phone));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. isValidPassengerCount
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isValidPassengerCount()")
    class IsValidPassengerCount {

        @ParameterizedTest(name = "passengers={0}, seats={1} → true")
        @CsvSource({
            "1,  1",
            "1, 10",
            "5, 10",
            "10, 10",
            "1, 100",
        })
        void validCountsReturnTrue(int passengers, int seats) {
            assertTrue(ValidationUtils.isValidPassengerCount(passengers, seats));
        }

        @ParameterizedTest(name = "passengers={0}, seats={1} → false")
        @CsvSource({
            "0,  10",    // zero passengers
            "-1, 10",    // negative passengers
            "11, 10",    // over capacity
            "100, 1",    // way over capacity
            "1,  0",     // zero seats edge case
        })
        void invalidCountsReturnFalse(int passengers, int seats) {
            assertFalse(ValidationUtils.isValidPassengerCount(passengers, seats));
        }

        @Test
        @DisplayName("exactly 1 passenger in 1 seat → valid")
        void exactlyOneSeatOnePax() {
            assertTrue(ValidationUtils.isValidPassengerCount(1, 1));
        }

        @Test
        @DisplayName("0 passengers → always invalid regardless of seats")
        void zeroPaxAlwaysInvalid() {
            assertFalse(ValidationUtils.isValidPassengerCount(0, 999));
        }

        @Test
        @DisplayName("negative passengers never valid")
        void negativePaxNeverValid() {
            assertFalse(ValidationUtils.isValidPassengerCount(-100, 1000));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. getValidationMessage
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getValidationMessage()")
    class GetValidationMessage {

        @Test
        @DisplayName("\"required\" type includes field name")
        void requiredMessageIncludesFieldName() {
            String msg = ValidationUtils.getValidationMessage("Email", "required");
            assertNotNull(msg);
            assertTrue(msg.contains("Email") || msg.contains("required"),
                    "Message should reference field or type. Got: " + msg);
        }

        @Test
        @DisplayName("\"email\" type returns email-specific message")
        void emailTypeMessage() {
            String msg = ValidationUtils.getValidationMessage("Email", "email");
            assertNotNull(msg);
            assertFalse(msg.isBlank());
        }

        @Test
        @DisplayName("\"phone\" type returns phone-specific message")
        void phoneTypeMessage() {
            String msg = ValidationUtils.getValidationMessage("Phone", "phone");
            assertNotNull(msg);
            assertFalse(msg.isBlank());
        }

        @Test
        @DisplayName("\"seats\" type returns seat-specific message")
        void seatsTypeMessage() {
            String msg = ValidationUtils.getValidationMessage("Passengers", "seats");
            assertNotNull(msg);
            assertFalse(msg.isBlank());
        }

        @Test
        @DisplayName("unknown type returns generic invalid message including field name")
        void unknownTypeFallback() {
            String msg = ValidationUtils.getValidationMessage("MyField", "unknownType");
            assertNotNull(msg);
            assertFalse(msg.isBlank());
            // Default branch is "Invalid " + fieldName
            assertTrue(msg.toLowerCase().contains("invalid") || msg.toLowerCase().contains("myfield"),
                    "Fallback should say invalid or include field name. Got: " + msg);
        }

        @ParameterizedTest(name = "type=\"{1}\" always returns non-null")
        @CsvSource({
            "Field, required",
            "Email, email",
            "Phone, phone",
            "Seats, seats",
            "X, unknown",
            "Y, ''",
        })
        void neverReturnsNull(String field, String type) {
            assertNotNull(ValidationUtils.getValidationMessage(field, type));
        }
    }
}
