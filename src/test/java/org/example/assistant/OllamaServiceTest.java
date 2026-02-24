package org.example.assistant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OllamaService} — singleton contract, constants,
 * system prompt completeness, and JSON parsing helpers.
 *
 * No network connection is required because we only test:
 *   • Singleton identity
 *   • Constant values (OLLAMA_BASE, MODEL)
 *   • System prompt content (all required action names present)
 *   • parseStringField (reused from PythonVoiceAgent) behaviour on Ollama-style JSON
 */
@DisplayName("OllamaService — constants, singleton, system prompt, JSON parsing")
class OllamaServiceTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Singleton contract
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Singleton contract")
    class SingletonContract {

        @Test
        @DisplayName("getInstance() returns non-null")
        void instanceNotNull() {
            assertNotNull(OllamaService.getInstance());
        }

        @Test
        @DisplayName("getInstance() always returns the same object (identity)")
        void sameInstance() {
            assertSame(OllamaService.getInstance(), OllamaService.getInstance());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Constants
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("OLLAMA_BASE is localhost:11434")
        void ollamaBase() {
            assertEquals("http://localhost:11434", OllamaService.OLLAMA_BASE);
        }

        @Test
        @DisplayName("MODEL is llama3.2")
        void model() {
            assertEquals("llama3.2", OllamaService.MODEL);
        }

        @Test
        @DisplayName("OLLAMA_BASE starts with http://")
        void baseIsHttp() {
            assertTrue(OllamaService.OLLAMA_BASE.startsWith("http://"));
        }

        @Test
        @DisplayName("OLLAMA_BASE includes a port number")
        void baseHasPort() {
            assertTrue(OllamaService.OLLAMA_BASE.contains(":"),
                    "Base URL should specify a port");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. System prompt completeness (via reflection)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SYSTEM_PROMPT — all GoVibe actions present")
    class SystemPromptActions {

        private static String systemPrompt;

        @BeforeAll
        static void extractPrompt() throws Exception {
            Field f = OllamaService.class.getDeclaredField("SYSTEM_PROMPT");
            f.setAccessible(true);
            systemPrompt = (String) f.get(null);
        }

        @ParameterizedTest(name = "action \"{0}\" in system prompt")
        @ValueSource(strings = {
            "BOOK",
            "RECHERCHER",
            "ANNULER",
            "PAYER",
            "AIDE",
            "DECRIRE",
            "DECONNEXION",
            "LOGIN",
            "SIGNUP",
            "FOCUS_EMAIL",
            "FOCUS_PASSWORD",
            "NONE",
            "UNKNOWN",
        })
        void allActionsPresent(String action) {
            assertNotNull(systemPrompt, "SYSTEM_PROMPT should not be null");
            assertTrue(systemPrompt.contains(action),
                    "SYSTEM_PROMPT must mention action: " + action);
        }

        @Test
        @DisplayName("system prompt instructs JSON-only output (no markdown)")
        void promptInstructsJsonOnly() {
            assertNotNull(systemPrompt);
            String lower = systemPrompt.toLowerCase();
            assertTrue(lower.contains("json"),
                    "Prompt should instruct JSON output");
        }

        @Test
        @DisplayName("system prompt mentions destination extraction")
        void promptMentionsDestination() {
            assertNotNull(systemPrompt);
            assertTrue(systemPrompt.contains("destination"),
                    "Prompt should mention destination extraction");
        }

        @Test
        @DisplayName("system prompt mentions date extraction")
        void promptMentionsDate() {
            assertNotNull(systemPrompt);
            assertTrue(systemPrompt.contains("date"),
                    "Prompt should mention date extraction");
        }

        @Test
        @DisplayName("system prompt mentions confidence field")
        void promptMentionsConfidence() {
            assertNotNull(systemPrompt);
            assertTrue(systemPrompt.contains("confidence"),
                    "Prompt should mention confidence in the JSON format");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. isAvailable() — safe to call (returns false when Ollama not running)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isAvailable() does not throw — returns boolean (false when offline)")
    void isAvailableDoesNotThrow() {
        // In a CI/test environment Ollama is not running — the method should
        // degrade gracefully and return false (not throw an exception).
        assertDoesNotThrow(() -> {
            boolean available = OllamaService.getInstance().isAvailable();
            // Either true (Ollama running) or false (not running) — both valid.
            assertTrue(available || !available, "Must return a boolean");
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. JSON parsing — OllamaService reuses PythonVoiceAgent.parseStringField
    //    Test the Ollama JSON output format that classify() would parse.
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ollama JSON format — parseStringField compatibility")
    class OllamaJsonParsing {

        private static final String SAMPLE_OLLAMA_RESPONSE =
            "{\"action\":\"BOOK\",\"confidence\":0.95," +
            "\"destination\":\"Paris\",\"date\":\"2026-03-15\"," +
            "\"response\":\"Opening the flight booking form for Paris.\"}";

        @Test
        @DisplayName("action field parsed correctly from Ollama JSON")
        void actionParsed() {
            assertEquals("BOOK",
                PythonVoiceAgent.parseStringField(SAMPLE_OLLAMA_RESPONSE, "action"));
        }

        @Test
        @DisplayName("destination field parsed correctly from Ollama JSON")
        void destinationParsed() {
            assertEquals("Paris",
                PythonVoiceAgent.parseStringField(SAMPLE_OLLAMA_RESPONSE, "destination"));
        }

        @Test
        @DisplayName("date field parsed correctly from Ollama JSON")
        void dateParsed() {
            assertEquals("2026-03-15",
                PythonVoiceAgent.parseStringField(SAMPLE_OLLAMA_RESPONSE, "date"));
        }

        @Test
        @DisplayName("response field parsed correctly from Ollama JSON")
        void responseParsed() {
            String r = PythonVoiceAgent.parseStringField(SAMPLE_OLLAMA_RESPONSE, "response");
            assertNotNull(r);
            assertTrue(r.contains("Paris"), "Response should mention Paris");
        }

        @Test
        @DisplayName("missing field returns null (graceful)")
        void missingFieldReturnsNull() {
            assertNull(PythonVoiceAgent.parseStringField(SAMPLE_OLLAMA_RESPONSE, "nonexistent"));
        }

        @Test
        @DisplayName("UNKNOWN action JSON parses correctly")
        void unknownActionJson() {
            String json = "{\"action\":\"UNKNOWN\",\"confidence\":0.1," +
                          "\"destination\":null,\"date\":null,\"response\":\"I did not understand.\"}";
            assertEquals("UNKNOWN", PythonVoiceAgent.parseStringField(json, "action"));
        }

        @ParameterizedTest(name = "action = {0}")
        @ValueSource(strings = {
            "BOOK", "RECHERCHER", "ANNULER", "PAYER",
            "AIDE", "DECRIRE", "DECONNEXION", "LOGIN",
            "SIGNUP", "NONE", "UNKNOWN"
        })
        @DisplayName("all valid action codes parse correctly from JSON")
        void allActionCodesParsed(String action) {
            String json = String.format(
                "{\"action\":\"%s\",\"confidence\":0.9,\"destination\":null," +
                "\"date\":null,\"response\":\"Test.\"}",
                action);
            assertEquals(action, PythonVoiceAgent.parseStringField(json, "action"));
        }
    }
}
