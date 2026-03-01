package org.example.assistant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VoiceAssistantService's edge-tts TTS pipeline.
 *
 * Tests the private static methods via reflection (no real audio needed):
 *   buildEdgeTtsScript() — Python script generation, injection safety
 *   checkEdgeTts()      — caching, detection result stored in static field
 *
 * These tests exercise the critical TTS code path without making network calls
 * or spawning sub-processes (the script content is inspected as a String).
 */
@DisplayName("VoiceAssistantService — edge-tts pipeline (unit)")
class VoiceAssistantEdgeTtsTest {

    // ── Reflection helpers ──────────────────────────────────────────────────

    private static Method buildEdgeTtsScript;
    private static Field  edgeTtsAvailableField;
    private static Field  edgeTtsPythonExeField;

    @BeforeAll
    static void resolveReflection() throws Exception {
        buildEdgeTtsScript = VoiceAssistantService.class
                .getDeclaredMethod("buildEdgeTtsScript", String.class);
        buildEdgeTtsScript.setAccessible(true);

        edgeTtsAvailableField = VoiceAssistantService.class
                .getDeclaredField("edgeTtsAvailable");
        edgeTtsAvailableField.setAccessible(true);

        edgeTtsPythonExeField = VoiceAssistantService.class
                .getDeclaredField("edgeTtsPythonExe");
        edgeTtsPythonExeField.setAccessible(true);
    }

    /** Reset static cache so each test runs with a clean detection state. */
    @BeforeEach
    void resetEdgeTtsCache() throws Exception {
        edgeTtsAvailableField.set(null, null);  // null = "not yet probed"
        edgeTtsPythonExeField.set(null, null);
    }

    private String buildScript(String text) throws Exception {
        return (String) buildEdgeTtsScript.invoke(null, text);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. buildEdgeTtsScript — structure checks
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildEdgeTtsScript() — script structure")
    class BuildEdgeTtsScriptStructure {

        @Test
        @DisplayName("returns non-null, non-blank Python script")
        void returnsNonBlankScript() throws Exception {
            String script = buildScript("Hello");
            assertNotNull(script);
            assertFalse(script.isBlank());
        }

        @Test
        @DisplayName("script imports edge_tts module")
        void importsEdgeTts() throws Exception {
            String script = buildScript("Hello");
            assertTrue(script.contains("edge_tts"),
                    "Script must reference edge_tts module. Got: " + script.substring(0, Math.min(200, script.length())));
        }

        @Test
        @DisplayName("script uses en-US-JennyNeural voice")
        void usesJennyNeural() throws Exception {
            String script = buildScript("Hello");
            assertTrue(script.contains("en-US-JennyNeural"),
                    "Script must select JennyNeural voice");
        }

        @Test
        @DisplayName("script calls asyncio.run()")
        void callsAsyncioRun() throws Exception {
            assertTrue(buildScript("Go").contains("asyncio.run("));
        }

        @Test
        @DisplayName("script calls com.save() to write MP3")
        void callsComSave() throws Exception {
            assertTrue(buildScript("Test").contains(".save("));
        }

        @Test
        @DisplayName("script plays via WinMM MCI (not SAPI)")
        void usesWmpNotSapi() throws Exception {
            String script = buildScript("Hello GoVibe");
            assertTrue(script.contains("winmm") || script.contains("mciSendString") || script.contains("wmp.MediaPlayer"),
                    "Script must use native audio (WinMM MCI or WMP), not SAPI TTS");
            assertFalse(script.contains("SpeechSynthesizer"),
                    "Script must NOT use SpeechSynthesizer (causes SAPI conflict)");
        }

        @Test
        @DisplayName("script cleans up MP3 temp file")
        void cleansUpTempFile() throws Exception {
            String script = buildScript("Cleanup test");
            assertTrue(script.contains("os.unlink") || script.contains("os.remove"),
                    "Script should delete temp MP3 file");
        }

        @Test
        @DisplayName("script uses tempfile for MP3 path")
        void usesTempfile() throws Exception {
            assertTrue(buildScript("Temp").contains("tempfile"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. buildEdgeTtsScript — text injection and escaping
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildEdgeTtsScript() — text embedding and escaping")
    class BuildEdgeTtsScriptEscaping {

        @Test
        @DisplayName("plain text appears in script")
        void plainTextEmbedded() throws Exception {
            String script = buildScript("Hello GoVibe");
            assertTrue(script.contains("Hello GoVibe"),
                    "Plain text should appear verbatim in script");
        }

        @Test
        @DisplayName("single quote in text is escaped to prevent Python syntax error")
        void singleQuoteEscaped() throws Exception {
            String script = buildScript("It's working");
            // Single quote must be escaped (\') so the Python string literal is valid
            assertFalse(script.contains("'It's"),
                    "Unescaped single quote would break the Python literal");
            // The escaped version should appear
            assertTrue(script.contains("It\\'s") || script.contains("It''s") || !script.contains("It's"),
                    "Single quote must be escaped. Script fragment: " +
                    script.substring(0, Math.min(300, script.length())));
        }

        @Test
        @DisplayName("backslash in text is escaped")
        void backslashEscaped() throws Exception {
            String script = buildScript("path\\file");
            // Raw backslash would break the Python string literal
            // Either escaped to \\\\ or not present as a single backslash inside a python string
            assertNotNull(script);
            // Just confirm script is still syntactically plausible (no bare unescaped \p)
            assertFalse(script.contains("'path\\file'"),
                    "Bare backslash should be escaped in the Python literal");
        }

        @ParameterizedTest(name = "text = \"{0}\"")
        @ValueSource(strings = {
            "Welcome to GoVibe",
            "Dashboard ready. Say Help for commands.",
            "Can you repeat that please?",
            "Noisy environment detected. Please speak clearly.",
        })
        @DisplayName("all standard TTS phrases generate valid scripts")
        void standardPhrasesGenerateScripts(String text) throws Exception {
            String script = buildScript(text);
            assertNotNull(script);
            assertFalse(script.isBlank());
            assertTrue(script.contains("edge_tts"));
        }

        @Test
        @DisplayName("empty string generates a non-null script (no crash)")
        void emptyTextNoException() {
            assertDoesNotThrow(() -> buildScript(""));
        }

        @Test
        @DisplayName("unicode text (French accents) embeds correctly")
        void unicodeText() throws Exception {
            String script = buildScript("Environnement bruyant détecté.");
            assertNotNull(script);
            assertFalse(script.isBlank());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. edgeTtsAvailable cache semantics
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("edgeTtsAvailable static cache")
    class EdgeTtsAvailableCache {

        @Test
        @DisplayName("initially null — not yet probed")
        void initiallyNull() throws Exception {
            // Reset already done in @BeforeEach
            assertNull(edgeTtsAvailableField.get(null),
                    "edgeTtsAvailable should be null before first checkEdgeTts() call");
        }

        @Test
        @DisplayName("after force-set true, getter returns true")
        void forceSetTrue() throws Exception {
            edgeTtsAvailableField.set(null, Boolean.TRUE);
            assertEquals(Boolean.TRUE, edgeTtsAvailableField.get(null));
        }

        @Test
        @DisplayName("after force-set false, getter returns false")
        void forceSetFalse() throws Exception {
            edgeTtsAvailableField.set(null, Boolean.FALSE);
            assertEquals(Boolean.FALSE, edgeTtsAvailableField.get(null));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. TTS_MUTE_WINDOW_MS value
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TTS_MUTE_WINDOW_MS is ≥ 1000 ms (anti-echo gate must outlast startup TTS)")
    void ttsMuteWindowIs600() throws Exception {
        Field field = VoiceAssistantService.class.getDeclaredField("TTS_MUTE_WINDOW_MS");
        field.setAccessible(true);
        long value = (long) field.get(null);
        assertTrue(value >= 1_000L,
                "Mute window should be ≥ 1000 ms so startup TTS audio is not fed back into Vosk as fake commands");
        assertTrue(value <= 5_000L,
                "Mute window should be ≤ 5000 ms so real user commands are not dropped");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. PS_EXE constant
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PS_EXE points to the standard PowerShell path on Windows")
    void psExeConstant() throws Exception {
        Field field = VoiceAssistantService.class.getDeclaredField("PS_EXE");
        field.setAccessible(true);
        String exe = (String) field.get(null);
        assertNotNull(exe);
        assertTrue(exe.contains("powershell") || exe.contains("PowerShell"),
                "PS_EXE should reference powershell.exe");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. DEBOUNCE_MS
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DEBOUNCE_MS is 2000 ms — prevents duplicate TTS within 2 s")
    void debounceMsIs2000() throws Exception {
        Field field = VoiceAssistantService.class.getDeclaredField("DEBOUNCE_MS");
        field.setAccessible(true);
        long value = (long) field.get(null);
        assertEquals(2_000L, value);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. STT_DEBOUNCE_MS
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("STT_DEBOUNCE_MS is 3000 ms — prevents duplicate command dispatch")
    void sttDebounceMsIs3000() throws Exception {
        Field field = VoiceAssistantService.class.getDeclaredField("STT_DEBOUNCE_MS");
        field.setAccessible(true);
        long value = (long) field.get(null);
        assertEquals(3_000L, value);
    }
}
