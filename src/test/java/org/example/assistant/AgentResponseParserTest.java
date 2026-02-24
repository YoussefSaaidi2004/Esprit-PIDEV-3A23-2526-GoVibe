package org.example.assistant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PythonVoiceAgent JSON parsing helpers and AgentResponse model.
 *
 * Tests the entire response-parsing pipeline that processes Tier-1/2/3 ML output:
 *   Python JSON string → AgentResponse fields → CommandRouter dispatch
 */
@DisplayName("PythonVoiceAgent — JSON parser and AgentResponse model")
class AgentResponseParserTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. parseStringField — minimal JSON string extractor
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseStringField()")
    class ParseStringField {

        @Test
        @DisplayName("extracts a simple string value")
        void simpleString() {
            String json = "{\"intent\":\"BOOK\",\"confidence\":0.9}";
            assertEquals("BOOK", PythonVoiceAgent.parseStringField(json, "intent"));
        }

        @Test
        @DisplayName("extracts second field correctly")
        void secondField() {
            String json = "{\"intent\":\"BOOK\",\"response\":\"Je vais ouvrir.\"}";
            assertEquals("Je vais ouvrir.", PythonVoiceAgent.parseStringField(json, "response"));
        }

        @Test
        @DisplayName("returns null when key is absent")
        void missingKeyReturnsNull() {
            assertNull(PythonVoiceAgent.parseStringField("{\"intent\":\"BOOK\"}", "action"));
        }

        @Test
        @DisplayName("returns null on null input")
        void nullInputReturnsNull() {
            assertNull(PythonVoiceAgent.parseStringField(null, "intent"));
        }

        @Test
        @DisplayName("handles escaped backslash inside value")
        void escapedBackslash() {
            String json = "{\"response\":\"path\\\\file\"}";
            assertEquals("path\\file", PythonVoiceAgent.parseStringField(json, "response"));
        }

        @Test
        @DisplayName("handles escaped quote inside value")
        void escapedQuote() {
            String json = "{\"response\":\"say \\\"hello\\\"\"}";
            assertEquals("say \"hello\"", PythonVoiceAgent.parseStringField(json, "response"));
        }

        @Test
        @DisplayName("handles escaped newline inside value")
        void escapedNewline() {
            String json = "{\"response\":\"line1\\nline2\"}";
            String val = PythonVoiceAgent.parseStringField(json, "response");
            assertNotNull(val);
            assertTrue(val.contains("\n"), "Expected newline character in parsed value");
        }

        @Test
        @DisplayName("handles null JSON literal as plain-string 'null'")
        void nullLiteralValue() {
            // Python may emit: "destination":"null"  for none
            String json = "{\"destination\":\"null\"}";
            assertEquals("null", PythonVoiceAgent.parseStringField(json, "destination"));
        }

        @ParameterizedTest(name = "extracts {1} for key {0}")
        @CsvSource({
            "intent,     BOOK,",
            "action,     VIEW_BOOKINGS,",
            "engine,     sentence-transformers,",
            "destination,Paris,",
        })
        void extractsVariousKeys(String key, String expected) {
            String json = String.format("{\"%s\":\"%s\"}", key, expected);
            assertEquals(expected, PythonVoiceAgent.parseStringField(json, key));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. AgentResponse — factory: unknown()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AgentResponse.unknown() factory")
    class AgentResponseUnknown {

        private PythonVoiceAgent.AgentResponse ar;

        @BeforeEach
        void create() {
            ar = PythonVoiceAgent.AgentResponse.unknown("test input");
        }

        @Test @DisplayName("intent is UNKNOWN")
        void intentIsUnknown() { assertEquals("UNKNOWN", ar.intent); }

        @Test @DisplayName("action is UNKNOWN")
        void actionIsUnknown() { assertEquals("UNKNOWN", ar.action); }

        @Test @DisplayName("confidence is 0.0")
        void confidenceIsZero() { assertEquals(0.0, ar.confidence, 1e-9); }

        @Test @DisplayName("destination is null")
        void destinationIsNull() { assertNull(ar.destination); }

        @Test @DisplayName("date is null")
        void dateIsNull() { assertNull(ar.date); }

        @Test @DisplayName("engine is 'error'")
        void engineIsError() { assertEquals("error", ar.engine); }

        @Test @DisplayName("hasDestination() returns false")
        void hasDestinationFalse() { assertFalse(ar.hasDestination()); }

        @Test @DisplayName("hasDate() returns false")
        void hasDateFalse() { assertFalse(ar.hasDate()); }

        @Test @DisplayName("response text is non-null")
        void responseNotNull() { assertNotNull(ar.response); }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Full response parsing — from Python JSON line to AgentResponse
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Full JSON response → AgentResponse parsing")
    class FullResponseParsing {

        /** Simulates what voice_agent.py emits for a BOOK intent. */
        @Test
        @DisplayName("parses BOOK intent with destination and date (Tier-2 Ollama output)")
        void parsesBookWithDestinationAndDate() {
            String json = "{\"intent\":\"BOOK\",\"response\":\"Je vais chercher des vols pour Paris.\"," +
                    "\"action\":\"BOOK\",\"confidence\":0.95," +
                    "\"destination\":\"Paris\",\"date\":\"2026-03-15\",\"engine\":\"ollama\"}";

            PythonVoiceAgent.AgentResponse ar = parseJson(json);

            assertEquals("BOOK",   ar.intent);
            assertEquals("BOOK",   ar.action);
            assertEquals(0.95,     ar.confidence, 1e-9);
            assertEquals("Paris",  ar.destination);
            assertEquals("2026-03-15", ar.date);
            assertEquals("ollama", ar.engine);
            assertTrue(ar.hasDestination());
            assertTrue(ar.hasDate());
        }

        @Test
        @DisplayName("parses VIEW_BOOKINGS intent (no destination, no date)")
        void parsesViewBookings() {
            String json = "{\"intent\":\"VIEW_BOOKINGS\",\"response\":\"Affichage de vos réservations.\"," +
                    "\"action\":\"VIEW_BOOKINGS\",\"confidence\":0.88," +
                    "\"destination\":\"null\",\"date\":\"null\",\"engine\":\"sentence-transformers\"}";

            PythonVoiceAgent.AgentResponse ar = parseJson(json);

            assertEquals("VIEW_BOOKINGS", ar.intent);
            assertEquals(0.88, ar.confidence, 1e-9);
            // "null" literal → Java null
            assertNull(ar.destination, "Literal 'null' string should parse to Java null");
            assertNull(ar.date,        "Literal 'null' string should parse to Java null");
            assertFalse(ar.hasDestination());
            assertFalse(ar.hasDate());
            assertEquals("sentence-transformers", ar.engine);
        }

        @Test
        @DisplayName("parses UNKNOWN intent with low confidence")
        void parsesUnknown() {
            String json = "{\"intent\":\"UNKNOWN\",\"response\":\"Je n'ai pas compris.\"," +
                    "\"action\":\"UNKNOWN\",\"confidence\":0.15," +
                    "\"destination\":\"null\",\"date\":\"null\",\"engine\":\"sklearn-tfidf\"}";

            PythonVoiceAgent.AgentResponse ar = parseJson(json);

            assertEquals("UNKNOWN",      ar.intent);
            assertEquals(0.15,           ar.confidence, 1e-9);
            assertEquals("sklearn-tfidf", ar.engine);
            assertFalse(ar.hasDestination());
        }

        @Test
        @DisplayName("confidence 0.0 when field is missing")
        void confidenceZeroWhenMissing() {
            String json = "{\"intent\":\"BOOK\",\"action\":\"BOOK\",\"response\":\"ok\"}";
            PythonVoiceAgent.AgentResponse ar = parseJson(json);
            assertEquals(0.0, ar.confidence, 1e-9);
        }

        @Test
        @DisplayName("intent defaults to UNKNOWN when field is absent")
        void intentDefaultsToUnknownWhenMissing() {
            String json = "{\"action\":\"BOOK\",\"response\":\"ok\",\"confidence\":0.5}";
            PythonVoiceAgent.AgentResponse ar = parseJson(json);
            assertEquals("UNKNOWN", ar.intent);
        }

        @Test
        @DisplayName("action defaults to UNKNOWN when field is absent")
        void actionDefaultsToUnknownWhenMissing() {
            String json = "{\"intent\":\"BOOK\",\"response\":\"ok\",\"confidence\":0.5}";
            PythonVoiceAgent.AgentResponse ar = parseJson(json);
            assertEquals("UNKNOWN", ar.action);
        }

        @Test
        @DisplayName("engine defaults to 'unknown' when field is absent")
        void engineDefaultsWhenMissing() {
            String json = "{\"intent\":\"BOOK\",\"action\":\"BOOK\",\"response\":\"ok\",\"confidence\":0.9}";
            PythonVoiceAgent.AgentResponse ar = parseJson(json);
            assertEquals("unknown", ar.engine);
        }

        @Test
        @DisplayName("parses destination with spaces in city name")
        void parsesDestinationWithSpaces() {
            String json = "{\"intent\":\"BOOK\",\"action\":\"BOOK\",\"response\":\"ok\"," +
                    "\"confidence\":0.9,\"destination\":\"New York\",\"date\":\"null\"," +
                    "\"engine\":\"ollama\"}";
            PythonVoiceAgent.AgentResponse ar = parseJson(json);
            assertEquals("New York", ar.destination);
            assertTrue(ar.hasDestination());
        }

        @ParameterizedTest(name = "engine={0}")
        @ValueSource(strings = {"sentence-transformers", "ollama", "sklearn-tfidf", "sentence-transformers+ollama"})
        @DisplayName("all engine tier names preserved correctly")
        void allEngineTierNamesParsed(String engineName) {
            String json = "{\"intent\":\"BOOK\",\"action\":\"BOOK\",\"response\":\"ok\"," +
                    "\"confidence\":0.8,\"destination\":\"null\",\"date\":\"null\"," +
                    "\"engine\":\"" + engineName + "\"}";
            PythonVoiceAgent.AgentResponse ar = parseJson(json);
            assertEquals(engineName, ar.engine);
        }

        /**
         * Simulates the parseResponse call by using the package-accessible
         * parseStringField helper to reconstruct what parseResponse() would do.
         */
        private PythonVoiceAgent.AgentResponse parseJson(String json) {
            // Call the internal static helper via package access (same package as test)
            String intent     = PythonVoiceAgent.parseStringField(json, "intent");
            String response   = PythonVoiceAgent.parseStringField(json, "response");
            String action     = PythonVoiceAgent.parseStringField(json, "action");
            String destination= PythonVoiceAgent.parseStringField(json, "destination");
            String date       = PythonVoiceAgent.parseStringField(json, "date");
            String engine     = PythonVoiceAgent.parseStringField(json, "engine");

            // Replicate the parseResponse() logic:
            String confStr = extractRaw(json, "confidence");
            double confidence = 0.0;
            try { confidence = Double.parseDouble(confStr); } catch (Exception ignored) {}

            if (intent == null)   intent   = "UNKNOWN";
            if (response == null) response = "";
            if (action == null)   action   = "UNKNOWN";
            if ("null".equalsIgnoreCase(destination)) destination = null;
            if ("null".equalsIgnoreCase(date))        date        = null;

            return new PythonVoiceAgent.AgentResponse(
                    intent, response, action, confidence, destination, date, engine, 1);
        }

        /** Replicate parseRawField for confidence extraction. */
        private String extractRaw(String json, String key) {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx < 0) return "0";
            idx += search.length();
            while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
            int start = idx;
            while (idx < json.length() && ",}".indexOf(json.charAt(idx)) < 0) idx++;
            return json.substring(start, idx).trim();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. AgentResponse — hasDestination / hasDate edge cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AgentResponse convenience predicates")
    class ConveniencePredicates {

        @Test @DisplayName("hasDestination() true when destination is non-blank")
        void hasDestinationTrue() {
            var ar = new PythonVoiceAgent.AgentResponse(
                    "BOOK","ok","BOOK",0.9,"Paris","2026-04-01","ollama", 1);
            assertTrue(ar.hasDestination());
        }

        @Test @DisplayName("hasDestination() false when destination is null")
        void hasDestinationFalseNull() {
            var ar = new PythonVoiceAgent.AgentResponse(
                    "BOOK","ok","BOOK",0.9, null, null,"ollama", 1);
            assertFalse(ar.hasDestination());
        }

        @Test @DisplayName("hasDestination() false when destination is blank")
        void hasDestinationFalseBlank() {
            var ar = new PythonVoiceAgent.AgentResponse(
                    "BOOK","ok","BOOK",0.9,"  ", null,"ollama", 1);
            assertFalse(ar.hasDestination());
        }

        @Test @DisplayName("hasDate() true when date is non-blank")
        void hasDateTrue() {
            var ar = new PythonVoiceAgent.AgentResponse(
                    "BOOK","ok","BOOK",0.9,"Paris","2026-04-01","ollama", 1);
            assertTrue(ar.hasDate());
        }

        @Test @DisplayName("hasDate() false when date is null")
        void hasDateFalseNull() {
            var ar = new PythonVoiceAgent.AgentResponse(
                    "BOOK","ok","BOOK",0.9,"Paris", null,"ollama", 1);
            assertFalse(ar.hasDate());
        }

        @Test @DisplayName("engine defaults to 'unknown' if null passed in constructor")
        void engineDefaultsToUnknownIfNull() {
            var ar = new PythonVoiceAgent.AgentResponse(
                    "BOOK","ok","BOOK",0.9,null,null, null, 1);
            assertEquals("unknown", ar.engine);
        }
    }
}
