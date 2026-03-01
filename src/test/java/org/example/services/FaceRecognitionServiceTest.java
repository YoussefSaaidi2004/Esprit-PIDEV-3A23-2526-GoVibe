package org.example.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FaceRecognitionService}.
 *
 * <p>Tests focus on the pure-Java logic that can be exercised without
 * launching an actual camera or Python process:
 * <ul>
 *   <li>Service construction (sanity smoke-test)</li>
 *   <li>{@link FaceRecognitionService#verifyFace(String)} short-circuits
 *       immediately on null/blank input — <em>no Python process is spawned</em></li>
 * </ul>
 *
 * <p>Tests that require real camera capture, Python 3.11, opencv, or mediapipe
 * are out of scope for unit tests and are validated through manual integration
 * testing only.
 */
class FaceRecognitionServiceTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Construction
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Service construction")
    class Construction {

        @Test
        @DisplayName("FaceRecognitionService can be instantiated without throwing")
        void instantiation() {
            assertDoesNotThrow(FaceRecognitionService::new,
                    "Constructor must not throw — pythonCommand discovery is lazy");
        }

        @Test
        @DisplayName("service instance is non-null")
        void instanceIsNonNull() {
            assertNotNull(new FaceRecognitionService());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. verifyFace() — null/blank/empty guard (no Python process)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyFace() guard — null / blank encoding returns false immediately")
    class VerifyFaceGuard {

        private FaceRecognitionService service;

        @BeforeEach
        void setUp() {
            service = new FaceRecognitionService();
        }

        @ParameterizedTest(name = "verifyFace(''{0}'') → false (no Python spawned)")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", "  \t  "})
        void nullOrBlankEncodingReturnsFalse(String encoding) {
            // Must return false within milliseconds — no process is spawned.
            long start = System.currentTimeMillis();
            boolean result = service.verifyFace(encoding);
            long elapsed = System.currentTimeMillis() - start;

            assertFalse(result, "verifyFace must return false for absent encoding");
            assertTrue(elapsed < 500,
                    "Guard must be instant (< 500 ms) — no process should be spawned; actual: " + elapsed + " ms");
        }

        @Test
        @DisplayName("verifyFace(null) → false without NullPointerException")
        void nullDoesNotThrow() {
            assertDoesNotThrow(() -> service.verifyFace(null));
            assertFalse(service.verifyFace(null));
        }

        @Test
        @DisplayName("verifyFace(\"\") → false without exception")
        void emptyDoesNotThrow() {
            assertDoesNotThrow(() -> service.verifyFace(""));
            assertFalse(service.verifyFace(""));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. verifyFace() — malformed JSON does not crash the service
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyFace() — service is resilient to runtime errors")
    class VerifyFaceResilience {

        @Test
        @DisplayName("multiple sequential null calls do not leave the service in a broken state")
        void multipleNullCallsAreIdempotent() {
            FaceRecognitionService svc = new FaceRecognitionService();
            for (int i = 0; i < 10; i++) {
                assertFalse(svc.verifyFace(null),
                        "call #" + (i + 1) + " should still return false");
            }
        }

        @Test
        @DisplayName("verifyFace returns false (not throws) even when called with whitespace-only string")
        void whitespaceOnlyStringReturnsFalse() {
            FaceRecognitionService svc = new FaceRecognitionService();
            // "    ".trim().isEmpty() is true → immediate false guard
            boolean result = assertDoesNotThrow(() -> svc.verifyFace("    "));
            assertFalse(result);
        }
    }
}
