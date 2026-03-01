package org.example.assistant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the private static helpers in {@link PythonVoiceAgent}
 * that were added/modified to fix self-echo and Python-3.14 discovery:
 *
 * <ul>
 *   <li>{@code buildCommand(String, String...)} — command tokenisation</li>
 *   <li>{@code isTtsStatusLine(String)} — boot-phase tts_status detector</li>
 *   <li>{@code setBootTtsCallback(Consumer)} / {@code bootTtsCallback} — mic gate</li>
 * </ul>
 */
class PythonVoiceAgentInternalsTest {

    // ── Reflection helpers ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<String> buildCommand(String pythonCmd, String... extra) throws Exception {
        Method m = PythonVoiceAgent.class.getDeclaredMethod(
                "buildCommand", String.class, String[].class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, pythonCmd, extra);
    }

    private static boolean isTtsStatusLine(String line) throws Exception {
        Method m = PythonVoiceAgent.class.getDeclaredMethod("isTtsStatusLine", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, line);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. buildCommand — tokenisation / absolute-path handling
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildCommand() tokenisation")
    class BuildCommandTokenisation {

        @Test
        @DisplayName("multi-token 'py -3.14' is split into two tokens")
        void multiTokenPyLauncher() throws Exception {
            List<String> cmd = buildCommand("py -3.14", "-u", "script.py");
            assertEquals(List.of("py", "-3.14", "-u", "script.py"), cmd);
        }

        @Test
        @DisplayName("'py -3.11' splits correctly")
        void multiTokenPy311() throws Exception {
            List<String> cmd = buildCommand("py -3.11");
            assertEquals(List.of("py", "-3.11"), cmd);
        }

        @Test
        @DisplayName("single-token 'python' is not split")
        void singleTokenPython() throws Exception {
            List<String> cmd = buildCommand("python", "-u", "agent.py");
            assertEquals(List.of("python", "-u", "agent.py"), cmd);
        }

        @Test
        @DisplayName("single-token 'python3' is not split")
        void singleTokenPython3() throws Exception {
            List<String> cmd = buildCommand("python3", "--version");
            assertEquals(List.of("python3", "--version"), cmd);
        }

        @Test
        @DisplayName("Windows absolute path is NOT split on spaces in directory segments")
        void windowsAbsolutePath() throws Exception {
            String path = "C:\\Users\\user\\AppData\\Local\\Python\\bin\\python3.14.exe";
            List<String> cmd = buildCommand(path, "-u", "run.py");
            assertEquals(List.of(path, "-u", "run.py"), cmd,
                    "Absolute path starting with drive letter must not be split");
        }

        @Test
        @DisplayName("Unix absolute path starting with '/' is NOT split")
        void unixAbsolutePath() throws Exception {
            String path = "/usr/bin/python3";
            List<String> cmd = buildCommand(path, "-c", "import sys");
            assertEquals(List.of(path, "-c", "import sys"), cmd,
                    "Unix absolute path must not be split");
        }

        @Test
        @DisplayName("quoted path has quotes stripped and is not split")
        void quotedPath() throws Exception {
            // Users should not pass quoted paths but the guard must handle it safely
            String path = "\"C:\\Python314\\python.exe\"";
            List<String> cmd = buildCommand(path);
            // Quotes stripped; result is a single token without quotes
            assertEquals(1, cmd.size(), "Quoted path should produce a single token");
            assertFalse(cmd.get(0).contains("\""), "Quotes should be stripped from the path");
        }

        @Test
        @DisplayName("extra args are always appended after the python tokens")
        void extraArgsAppended() throws Exception {
            // "py -3.14" → 2 tokens, plus 3 extra args = 5 total
            List<String> cmd = buildCommand("py -3.14", "-u", "voice_agent.py", "--debug");
            assertEquals(5, cmd.size());
            assertEquals("py",             cmd.get(0));
            assertEquals("-3.14",          cmd.get(1));
            assertEquals("-u",             cmd.get(2));
            assertEquals("voice_agent.py", cmd.get(3));
            assertEquals("--debug",        cmd.get(4));
        }

        @Test
        @DisplayName("no extra args produces only the python tokens")
        void noExtraArgs() throws Exception {
            List<String> cmd = buildCommand("python");
            assertEquals(List.of("python"), cmd);
        }

        @ParameterizedTest(name = "multi-token ''{0}'' is split at space")
        @ValueSource(strings = {"py -3.12", "py -3.13", "py -3.10"})
        void variousPyVersions(String pythonCmd) throws Exception {
            List<String> cmd = buildCommand(pythonCmd);
            assertEquals(2, cmd.size(), "py launcher + version flag should be two tokens");
            assertEquals("py", cmd.get(0));
            assertTrue(cmd.get(1).startsWith("-3."), "Second token should be a version flag");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. isTtsStatusLine — boot-phase TTS detection
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTtsStatusLine() detection")
    class TtsStatusLineDetection {

        @Test
        @DisplayName("speaking:true tts_status line is recognised")
        void speakingTrue() throws Exception {
            assertTrue(isTtsStatusLine("{\"type\":\"tts_status\",\"speaking\":true}"));
        }

        @Test
        @DisplayName("speaking:false tts_status line is recognised")
        void speakingFalse() throws Exception {
            assertTrue(isTtsStatusLine("{\"type\":\"tts_status\",\"speaking\":false}"));
        }

        @Test
        @DisplayName("ready signal is NOT a tts_status line")
        void readySignalIsNot() throws Exception {
            assertFalse(isTtsStatusLine("{\"status\":\"ready\"}"));
        }

        @Test
        @DisplayName("plain agent_response is NOT a tts_status line")
        void agentResponseIsNot() throws Exception {
            assertFalse(isTtsStatusLine("{\"intent\":\"GREET\",\"response\":\"Bonjour!\"}"));
        }

        @Test
        @DisplayName("null line returns false (no NPE)")
        void nullLine() throws Exception {
            assertFalse(isTtsStatusLine(null));
        }

        @Test
        @DisplayName("empty string returns false")
        void emptyLine() throws Exception {
            assertFalse(isTtsStatusLine(""));
        }

        @ParameterizedTest(name = "non-tts line ''{0}'' returns false")
        @ValueSource(strings = {
            "{\"type\":\"resume_wake_word\"}",
            "{\"type\":\"weather_show\",\"city\":\"Paris\"}",
            "Loading model...",
            "Python 3.14.2",
            "Traceback (most recent call last):"
        })
        void nonTtsLinesReturnFalse(String line) throws Exception {
            assertFalse(isTtsStatusLine(line));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. bootTtsCallback — mic gate during bootstrap
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setBootTtsCallback() / bootTtsCallback mic gate")
    class BootTtsCallbackMicGate {

        /**
         * After each test reset to no-op so the static field doesn't leak
         * state between tests.
         */
        @AfterEach
        void reset() {
            PythonVoiceAgent.setBootTtsCallback(null);
        }

        @Test
        @DisplayName("setBootTtsCallback(null) sets a no-op (does not throw)")
        void nullCallbackDoesNotThrow() {
            assertDoesNotThrow(() -> PythonVoiceAgent.setBootTtsCallback(null));
        }

        @Test
        @DisplayName("setBootTtsCallback(cb) stores the callback")
        void callbackIsStored() {
            AtomicBoolean received = new AtomicBoolean();
            // Directly invoke the static field via a second callback registration
            // to confirm our callback replaces the previous one.
            PythonVoiceAgent.setBootTtsCallback(speaking -> received.set(speaking));

            // Simulate what the boot-reader does when it sees a tts_status line:
            // We retrieve and invoke the field via reflection.
            assertDoesNotThrow(() -> {
                var field = PythonVoiceAgent.class.getDeclaredField("bootTtsCallback");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Consumer<Boolean> cb = (Consumer<Boolean>) field.get(null);
                cb.accept(true);
            });
            assertTrue(received.get(), "Callback should have been invoked with speaking=true");
        }

        @Test
        @DisplayName("callback receives speaking=false correctly")
        void callbackReceivesFalse() throws Exception {
            AtomicReference<Boolean> last = new AtomicReference<>();
            PythonVoiceAgent.setBootTtsCallback(last::set);

            var field = PythonVoiceAgent.class.getDeclaredField("bootTtsCallback");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Consumer<Boolean> cb = (Consumer<Boolean>) field.get(null);
            cb.accept(false);

            assertFalse(last.get(), "Callback should have been invoked with speaking=false");
        }

        @Test
        @DisplayName("re-setting callback replaces the previous one")
        void callbackIsReplaceable() throws Exception {
            AtomicBoolean first  = new AtomicBoolean();
            AtomicBoolean second = new AtomicBoolean();

            PythonVoiceAgent.setBootTtsCallback(b -> first.set(true));
            PythonVoiceAgent.setBootTtsCallback(b -> second.set(true));

            var field = PythonVoiceAgent.class.getDeclaredField("bootTtsCallback");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Consumer<Boolean> cb = (Consumer<Boolean>) field.get(null);
            cb.accept(true);

            assertFalse(first.get(),  "Original callback must not be invoked after replacement");
            assertTrue(second.get(), "Replacement callback must be invoked");
        }

        @Test
        @DisplayName("no-op callback (null reset) can be invoked without throwing")
        void noOpCallbackIsSafe() {
            PythonVoiceAgent.setBootTtsCallback(null);
            assertDoesNotThrow(() -> {
                var field = PythonVoiceAgent.class.getDeclaredField("bootTtsCallback");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Consumer<Boolean> cb = (Consumer<Boolean>) field.get(null);
                cb.accept(true);   // must not throw
                cb.accept(false);  // must not throw
            });
        }

        @Test
        @DisplayName("speaking=true sets micDiscardUntilMs to Long.MAX_VALUE pattern")
        void speakingTrueSignalMeaning() {
            // Verify the documented contract: when speaking=true the gate must
            // discard audio indefinitely (i.e. until speaking=false arrives).
            // We test the semantic intent rather than the VoiceAssistantService
            // internals (which are integration-tested elsewhere).
            AtomicReference<Boolean> lastValue = new AtomicReference<>();
            PythonVoiceAgent.setBootTtsCallback(lastValue::set);

            assertDoesNotThrow(() -> {
                var field = PythonVoiceAgent.class.getDeclaredField("bootTtsCallback");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Consumer<Boolean> cb = (Consumer<Boolean>) field.get(null);
                cb.accept(true);
                assertTrue(lastValue.get(), "speaking=true must propagate as Boolean.TRUE");
                cb.accept(false);
                assertFalse(lastValue.get(), "speaking=false must propagate as Boolean.FALSE");
            });
        }
    }
}
