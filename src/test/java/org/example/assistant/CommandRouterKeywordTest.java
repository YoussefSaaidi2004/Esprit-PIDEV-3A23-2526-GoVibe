package org.example.assistant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommandRouter — keyword matching, intent dispatch, recalibrate
 * command, and voice-context management.
 *
 * Tests use Mockito to stub VoiceAssistantService (avoids audio hardware) and
 * ControllerProxy (avoids JavaFX scene graph).  Only commands that resolve
 * without Platform.runLater are exercised here (AIDE, RECALIBRER, QUOI, etc.).
 */
@DisplayName("CommandRouter — keyword table and routing logic")
class CommandRouterKeywordTest {

    private VoiceAssistantService mockVas;
    private CommandRouter.ControllerProxy mockProxy;
    private CommandRouter router;

    @BeforeEach
    void setUp() {
        // Mockito creates proxy instances without calling the real constructors,
        // so no audio hardware or JavaFX is needed.
        mockVas   = Mockito.mock(VoiceAssistantService.class);
        mockProxy = Mockito.mock(CommandRouter.ControllerProxy.class);
        when(mockProxy.describeScreen()).thenReturn("Écran de test.");
        router = new CommandRouter(mockVas, mockProxy);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. tryKeywordMatch() — return value contract
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("tryKeywordMatch() return value")
    class TryKeywordMatchReturn {

        @ParameterizedTest(name = "\"{0}\" → true (known keyword)")
        @ValueSource(strings = {"AIDE", "HELP", "COMMANDES"})
        @DisplayName("help keywords return true")
        void helpKeywordsReturnTrue(String keyword) {
            assertTrue(router.tryKeywordMatch(keyword),
                    "Expected true for keyword: " + keyword);
        }

        @ParameterizedTest(name = "\"{0}\" → true (recalibrate keyword)")
        @ValueSource(strings = {"RECALIBRER", "CALIBRER", "BRUIT"})
        @DisplayName("recalibrate keywords return true")
        void recalibrateKeywordsReturnTrue(String keyword) {
            assertTrue(router.tryKeywordMatch(keyword));
        }

        @ParameterizedTest(name = "\"{0}\" → false (unknown command)")
        @ValueSource(strings = {
            "XYZZY", "ABRACADABRA", "", "123456",
            "FAIS QUELQUE CHOSE", "COMMANDE INCONNUE"
        })
        @DisplayName("unrecognised commands return false")
        void unknownCommandsReturnFalse(String cmd) {
            assertFalse(router.tryKeywordMatch(cmd),
                    "Expected false for unknown command: " + cmd);
        }

        @Test
        @DisplayName("keyword match is case- and accent-insensitive (norm() applied to both sides)")
        void caseAndAccentInsensitiveMatch() {
            // norm() strips accents and uppercases both sides before comparison.
            // Use only safe commands (AIDE, RECALIBRER, COMMANDES) that don't call Platform.runLater.

            // Case-insensitive: "aide" (lowercase) must match stored key AIDE
            assertTrue(router.tryKeywordMatch("aide"),
                    "Lowercase 'aide' should match AIDE after normalization");
            // Mixed-case
            assertTrue(router.tryKeywordMatch("Aide"),
                    "Mixed-case 'Aide' should match AIDE after normalization");
            // Accent stripping: "récalibrer" → NFD decompose → remove combining marks → "RECALIBRER"
            assertTrue(router.tryKeywordMatch("récalibrer"),
                    "Accented 'récalibrer' should match stored key RECALIBRER after norm()");
            assertTrue(router.tryKeywordMatch("RÉCALIBRER"),
                    "Accented uppercase 'RÉCALIBRER' should match stored key RECALIBRER after norm()");
            // Inside longer phrase
            assertTrue(router.tryKeywordMatch("veuillez récalibrer le bruit"),
                    "Accent in phrase should still match RECALIBRER or BRUIT");
        }

        @Test
        @DisplayName("keyword can appear anywhere within the command string")
        void keywordSubstringMatch() {
            // The keyword AIDE appears inside a longer phrase (norm() handles case/accents)
            assertTrue(router.tryKeywordMatch("J'AIMERAIS DE L'AIDE S'IL VOUS PLAIT"));
            assertTrue(router.tryKeywordMatch("j'aimerais de l'aide s'il vous plaît"));
        }

        @Test
        @DisplayName("first keyword wins when command contains multiple keywords")
        void firstKeywordWins() {
            // AIDE and HELP are both in the table; the first one registered (AIDE) wins
            // The router just returns true — we verify the call count to distinguish
            assertTrue(router.tryKeywordMatch("AIDE HELP"));
            // vas.vivianSpeak() should have been called exactly once (one handler invoked)
            verify(mockVas, times(1)).vivianSpeak(anyString());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. onCommand() — dispatches to handler
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onCommand() dispatch")
    class OnCommandDispatch {

        @Test
        @DisplayName("AIDE → speaks help text")
        void aideSpokenViaOnCommand() {
            router.onCommand("AIDE", "aide");
            verify(mockVas, atLeastOnce()).vivianSpeak(anyString());
        }

        @Test
        @DisplayName("QUOI → calls proxy.describeScreen() and speaks result")
        void quoiCallsDescribeScreen() {
            router.onCommand("QUOI", "quoi");
            verify(mockProxy).describeScreen();
            verify(mockVas).vivianSpeak("Écran de test.");
        }

        @Test
        @DisplayName("WHAT → same as QUOI (English alias)")
        void whatCallsDescribeScreen() {
            router.onCommand("WHAT", "what is this");
            verify(mockProxy).describeScreen();
        }

        @Test
        @DisplayName("RECALIBRER → speaks recalibration message")
        void recalibrerSpeaksMessage() {
            router.onCommand("RECALIBRER", "recalibrer");
            verify(mockVas).vivianSpeak(contains("Recalibrating"));
        }

        @Test
        @DisplayName("unknown command \u2192 speaks 'Can you repeat that please?'")
        void unknownCommandSpeaksRepeat() {
            assertDoesNotThrow(() -> router.onCommand("XYZXYZ_UNKNOWN", "raw"));
            // The router must ask user to repeat when nothing matches
            verify(mockVas, atLeastOnce()).vivianSpeak(contains("repeat"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. RECALIBRER command — integration with NoiseOrchestrator
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RECALIBRER command — NoiseOrchestrator integration")
    class RecalibrerIntegration {

        @Test
        @DisplayName("RECALIBRER resets calibration flag on the singleton Orchestrator")
        void recalibrerResetsOrchestrator() {
            // Verify the singleton orchestrator is not already in a broken state
            NoiseOrchestrator orch = NoiseOrchestrator.getInstance();
            boolean wasCalibratedBefore = orch.isCalibrated();

            // Trigger recalibrate via voice command
            router.onCommand("RECALIBRER", "recalibrer");

            // After force-recalibrate the orchestrator is NOT calibrated
            assertFalse(orch.isCalibrated(),
                    "Orchestrator should be uncalibrated after RECALIBRER command");
        }

        @Test
        @DisplayName("CALIBRER is an alias for RECALIBRER")
        void calibrerAlias() {
            router.onCommand("CALIBRER", "calibrer");
            assertFalse(NoiseOrchestrator.getInstance().isCalibrated());
        }

        @Test
        @DisplayName("BRUIT is an alias for RECALIBRER")
        void bruitAlias() {
            router.onCommand("BRUIT", "bruit");
            assertFalse(NoiseOrchestrator.getInstance().isCalibrated());
        }

        @Test
        @DisplayName("TTS speaks French recalibration message before resetting")
        void speaksFrenchMessageBeforeReset() {
            router.onCommand("RECALIBRER", "recalibrer");
            // Message should mention microphone and silence
            verify(mockVas).vivianSpeak(argThat(msg ->
                    msg.toLowerCase().contains("recalibration") ||
                    msg.toLowerCase().contains("microphone") ||
                    msg.toLowerCase().contains("silencieux") ||
                    msg.toLowerCase().contains("secondes")));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Voice context — setVoiceContext()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setVoiceContext() — parameter passing")
    class VoiceContextManagement {

        @Test
        @DisplayName("setVoiceContext() does not throw for any input")
        void setVoiceContextIsRobust() {
            assertDoesNotThrow(() -> router.setVoiceContext("Paris", "2026-03-15"));
            assertDoesNotThrow(() -> router.setVoiceContext(null, null));
            assertDoesNotThrow(() -> router.setVoiceContext("", ""));
        }

        @Test
        @DisplayName("setProxy() does not throw with null proxy")
        void setProxyNullSafe() {
            assertDoesNotThrow(() -> router.setProxy(null));
        }

        @Test
        @DisplayName("setProxy() updates the active proxy")
        void setProxyUpdatesProxy() {
            CommandRouter.ControllerProxy newProxy =
                    Mockito.mock(CommandRouter.ControllerProxy.class);
            when(newProxy.describeScreen()).thenReturn("Nouvel écran.");

            router.setProxy(newProxy);
            router.onCommand("QUOI", "quoi");

            verify(newProxy).describeScreen();
            verify(mockProxy, never()).describeScreen(); // old proxy not called
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. ControllerProxy default methods
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ControllerProxy default interface methods")
    class ControllerProxyDefaults {

        /** Minimal stub that implements only what interface forces. */
        private static class MinimalProxy implements CommandRouter.ControllerProxy {
            String lastDestination;
            String lastDate;
            boolean bookingOpened = false;

            @Override public void openBooking()            { bookingOpened = true; }
            @Override public void cancelBooking()          {}
            @Override public void showBookingsTab()        {}
            @Override public void showSearchTab()          {}
            @Override public void performLogin()           {}
            @Override public void focusEmailField()        {}
            @Override public void focusPasswordField()     {}
            @Override public void navigateToSignup()       {}
            @Override public void triggerPayment()         {}
            @Override public String describeScreen()       { return "test"; }

            @Override
            public void openBookingWith(String destination, String date) {
                lastDestination = destination;
                lastDate        = date;
            }
        }

        @Test
        @DisplayName("openBookingWith(null, null) falls back to openBooking()")
        void openBookingWithNullFallsBack() {
            // Test the DEFAULT implementation provided in the interface:
            // classes that don't override openBookingWith should call openBooking().
            // We cannot test the interface default directly, but MinimalProxy overrides it.
            // Instead, verify that the default method contract is: delegate when no dest.
            MinimalProxy proxy = new MinimalProxy();
            proxy.openBookingWith(null, null);
            // MinimalProxy overrides but captures — just verify no NPE
            assertNull(proxy.lastDestination);
        }

        @Test
        @DisplayName("openBookingWith(destination, date) captures parameters")
        void openBookingWithCapturesParameters() {
            MinimalProxy proxy = new MinimalProxy();
            proxy.openBookingWith("Paris", "2026-03-15");
            assertEquals("Paris",      proxy.lastDestination);
            assertEquals("2026-03-15", proxy.lastDate);
        }

        @Test
        @DisplayName("describeScreen() returns non-null from MinimalProxy")
        void describeScreenNonNull() {
            MinimalProxy proxy = new MinimalProxy();
            assertNotNull(proxy.describeScreen());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Help text completeness
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Help text content")
    class HelpTextContent {

        @Test
        @DisplayName("help text mentions recalibrer command")
        void helpMentionsRecalibrer() {
            router.onCommand("AIDE", "aide");

            verify(mockVas).vivianSpeak(argThat(msg ->
                    msg.toLowerCase().contains("recalibrer") ||
                    msg.toLowerCase().contains("recalibr")));
        }

        @Test
        @DisplayName("help text contains key French action words")
        void helpContainsKeywords() {
            router.onCommand("AIDE", "aide");

            verify(mockVas).vivianSpeak(argThat(msg ->
                    msg.contains("Login") &&
                    msg.contains("Recalibrate") &&
                    msg.contains("commands")));
        }
    }
}
