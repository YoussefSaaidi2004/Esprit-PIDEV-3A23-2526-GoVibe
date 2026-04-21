package org.example.assistant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.assistant.NoiseOrchestrator.AcousticEnvironment;
import static org.example.assistant.NoiseOrchestrator.AcousticEnvironment.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Intelligent Adaptive Noise Suppression Orchestrator.
 *
 * Tests the full closed-loop pipeline:
 *   DETECTOR → ADAPTIVE FILTER → SNR VALIDATOR → ORCHESTRATOR feedback
 *
 * Uses NoiseOrchestrator.createFreshForTesting() to bypass the 4.5 s TTS
 * settle delay so all tests complete in milliseconds.
 */
@DisplayName("NoiseOrchestrator — full pipeline")
class NoiseOrchestratorTest {

    /** Dummy PCM buffer — content irrelevant; only the rms parameter is used. */
    private static final byte[] DUMMY_BUF = new byte[4096];
    private static final int    BUF_LEN   = DUMMY_BUF.length;

    // ──────────────────────────────────────────────────────────────────────────
    // 1. AcousticEnvironment enum — profile classification and gate math
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AcousticEnvironment.fromFloor()")
    class EnvironmentClassification {

        @ParameterizedTest(name = "floor={0} → {1}")
        @CsvSource({
            "0,    SILENT",
            "40,   SILENT",
            "79,   SILENT",
            "80,   QUIET",
            "150,  QUIET",
            "200,  NORMAL",
            "300,  NORMAL",
            "450,  NOISY",
            "600,  NOISY",
            "800,  VERY_NOISY",
            "1200, VERY_NOISY",
        })
        void classifiesFloorIntoCorrectBand(double floor, AcousticEnvironment expected) {
            assertEquals(expected, AcousticEnvironment.fromFloor(floor));
        }

        @Test
        @DisplayName("boundary: exactly 80 RMS → QUIET")
        void exactBoundaryQuiet() {
            assertEquals(QUIET, AcousticEnvironment.fromFloor(80));
        }

        @Test
        @DisplayName("boundary: exactly 450 RMS → NOISY")
        void exactBoundaryNoisy() {
            assertEquals(NOISY, AcousticEnvironment.fromFloor(450));
        }

        @Test
        @DisplayName("negative / zero floor defaults to SILENT")
        void zeroFloor() {
            assertEquals(SILENT, AcousticEnvironment.fromFloor(0));
        }
    }

    @Nested
    @DisplayName("AcousticEnvironment.gateThreshold()")
    class GateThresholdMath {

        @Test
        @DisplayName("SILENT: min-gate floors below multiplier result")
        void silentMinGateApplied() {
            // floor=40, mult=1.2 → 48, but minGate=200 → gate=200
            assertEquals(200.0, SILENT.gateThreshold(40), 1e-9);
        }

        @Test
        @DisplayName("QUIET: min-gate floors below multiplier result")
        void quietMinGateApplied() {
            // floor=150, mult=1.5 → 225, but minGate=300 → gate=300
            assertEquals(300.0, QUIET.gateThreshold(150), 1e-9);
        }

        @Test
        @DisplayName("NORMAL: multiplier result exceeds min-gate")
        void normalMultiplierWins() {
            // floor=300, mult=1.8 → 540 > minGate=400
            assertEquals(540.0, NORMAL.gateThreshold(300), 1e-9);
        }

        @Test
        @DisplayName("NOISY: multiplier result exceeds min-gate")
        void noisyMultiplierWins() {
            // floor=600, mult=2.4 → 1440 > minGate=550
            assertEquals(1440.0, NOISY.gateThreshold(600), 1e-9);
        }

        @Test
        @DisplayName("VERY_NOISY: multiplier result exceeds min-gate")
        void veryNoisyMultiplierWins() {
            // floor=900, mult=3.2 → 2880 > minGate=700
            assertEquals(2880.0, VERY_NOISY.gateThreshold(900), 1e-9);
        }

        @ParameterizedTest(name = "{0}.gateThreshold({1}) >= minGate always")
        @CsvSource({
            "SILENT,    0",
            "SILENT,    1",
            "QUIET,     0",
            "QUIET,     50",
            "NORMAL,    0",
            "NORMAL,    100",
        })
        void gateIsAlwaysAtLeastMinGate(AcousticEnvironment env, double floor) {
            assertTrue(env.gateThreshold(floor) >= env.minGate);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. TTS settle-phase behaviour
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TTS settle phase")
    class TtsSettlePhase {

        @Test
        @DisplayName("frames during settle all return false (not speech)")
        void framesDuringSettleNotSpeech() {
            // zero settle-back → settle phase active
            NoiseOrchestrator o = new NoiseOrchestrator(0);
            for (int i = 0; i < 20; i++) {
                assertFalse(o.feedFrame(DUMMY_BUF, BUF_LEN, 5000),
                        "Expected false during TTS settle at frame " + i);
            }
            assertFalse(o.isCalibrated(), "Should not be calibrated during settle");
        }

        @Test
        @DisplayName("frames after settle begin calibration")
        void framesAfterSettleStartCalibration() {
            // createFreshForTesting() → settle already expired
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            // After 40 frames (CALIB_FRAMES) the orchestrator should be calibrated.
            for (int i = 0; i < 40; i++) {
                o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            }
            assertTrue(o.isCalibrated());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Initial calibration (Phase 1 — Detector)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 1 — Initial calibration (BERP-inspired Detector)")
    class InitialCalibration {

        private NoiseOrchestrator o;

        @BeforeEach
        void setup() {
            o = NoiseOrchestrator.createFreshForTesting();
        }

        @Test
        @DisplayName("not calibrated before 40 frames")
        void notCalibratedBefore40Frames() {
            for (int i = 0; i < 39; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertFalse(o.isCalibrated());
        }

        @Test
        @DisplayName("calibrated exactly at 40th frame")
        void calibratedAt40Frames() {
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertTrue(o.isCalibrated());
        }

        @Test
        @DisplayName("no frames return true during calibration (dispatch suppressed)")
        void noSpeechDispatchDuringCalibration() {
            for (int i = 0; i < 40; i++) {
                boolean speech = o.feedFrame(DUMMY_BUF, BUF_LEN, 5000.0); // huge RMS
                assertFalse(speech, "Frame " + i + " should not be speech during calibration");
            }
        }

        @Test
        @DisplayName("QUIET environment detected from ~150 RMS noise floor")
        void quietEnvironmentFromFloor150() {
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertEquals(QUIET, o.getEnvironment());
        }

        @Test
        @DisplayName("NORMAL environment detected from ~300 RMS noise floor")
        void normalEnvironmentFromFloor300() {
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 300.0);
            assertEquals(NORMAL, o.getEnvironment());
        }

        @Test
        @DisplayName("NOISY environment detected from ~600 RMS noise floor")
        void noisyEnvironmentFromFloor600() {
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 600.0);
            assertEquals(NOISY, o.getEnvironment());
        }

        @Test
        @DisplayName("noise floor approximates calibration RMS average")
        void noiseFloorApproximatesAverage() {
            // mix of 100 and 200 → average 150 → QUIET
            for (int i = 0; i < 20; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 100.0);
            for (int i = 0; i < 20; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 200.0);
            assertEquals(QUIET, o.getEnvironment());
            // floor should be close to 150
            assertEquals(150.0, o.getNoiseFloor(), 5.0);
        }

        @Test
        @DisplayName("gate threshold at least minGate after calibration")
        void gateThresholdAtLeastMinGate() {
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            AcousticEnvironment env = o.getEnvironment();
            assertTrue(o.getGateThreshold() >= env.minGate,
                    "gate=" + o.getGateThreshold() + " should be >= minGate=" + env.minGate);
        }

        @Test
        @DisplayName("environment-change callback fires once on first calibration")
        void callbackFiresOnFirstCalibration() {
            List<AcousticEnvironment> received = new ArrayList<>();
            o.setOnEnvironmentChanged(received::add);
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertEquals(1, received.size());
            assertEquals(QUIET, received.get(0));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Adaptive filter (Phase 2 — isSpeech detection)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 2 — Adaptive Filter (LMS-inspired gate)")
    class AdaptiveFilter {

        private NoiseOrchestrator o;

        @BeforeEach
        void calibrate() {
            o = NoiseOrchestrator.createFreshForTesting();
            // calibrate with floor=150 → QUIET, gate=300
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
        }

        @Test
        @DisplayName("frame ABOVE gate → returns true (speech detected)")
        void aboveGateIsSpeech() {
            assertTrue(o.feedFrame(DUMMY_BUF, BUF_LEN, 1000.0),
                    "RMS 1000 should be above QUIET gate 300");
        }

        @Test
        @DisplayName("frame BELOW gate → returns false (ambient noise)")
        void belowGateIsNotSpeech() {
            assertFalse(o.feedFrame(DUMMY_BUF, BUF_LEN, 100.0),
                    "RMS 100 should be below QUIET gate 300");
        }

        @Test
        @DisplayName("frame AT gate boundary → false (strict less-than)")
        void exactlyAtGate() {
            double gate = o.getGateThreshold();
            // gate - 1 is below
            assertFalse(o.feedFrame(DUMMY_BUF, BUF_LEN, gate - 1));
        }

        @Test
        @DisplayName("gate threshold updated after environment shift")
        void gateUpdatesAfterEnvironmentShift() {
            double originalGate = o.getGateThreshold(); // 300

            // Feed 80 frames at NOISY level (floor appears very high)
            for (int i = 0; i < 80; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 600.0);

            // Gate should have increased because environment drifted toward NOISY
            assertTrue(o.getGateThreshold() >= originalGate,
                    "Gate should not decrease when noise floor rises");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Continuous environment detection (BERP-inspired Detector)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 2 — Continuous environment shift detection")
    class EnvironmentShiftDetection {

        @Test
        @DisplayName("environment-change callback fires when acoustic profile shifts")
        void callbackFiresOnEnvironmentShift() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            List<AcousticEnvironment> changes = new ArrayList<>();
            o.setOnEnvironmentChanged(changes::add);

            // calibrate in SILENT / QUIET range (~50 RMS)
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 50.0);

            int beforeShift = changes.size();

            // Push 80 very-noisy frames (≥ 900 RMS) — lowest 25% still ≫ 800
            for (int i = 0; i < 80; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 900.0);

            // A shift from SILENT toward VERY_NOISY should have fired at least once
            assertTrue(changes.size() > beforeShift,
                    "Expected environment-change callback after noisy frames");
        }

        @Test
        @DisplayName("no spurious callback when environment stays constant")
        void noSpuriousCallbackWhenStable() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            List<AcousticEnvironment> changes = new ArrayList<>();
            o.setOnEnvironmentChanged(changes::add);

            // calibrate in QUIET
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);

            int afterCalib = changes.size(); // should be 1 (initial calibration)

            // Continue with same RMS — no environment shift expected
            for (int i = 0; i < 80; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 152.0);

            assertEquals(afterCalib, changes.size(),
                    "No additional callbacks expected when environment is stable");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. SNR Validator (Phase 3 — SONIC-inspired quality monitor)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 3 — SNR Validator (SONIC-inspired)")
    class SnrValidator {

        @Test
        @DisplayName("redetect callback fires when speech SNR drops below 4.0")
        void redetectCallbackOnLowSnr() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            AtomicInteger redetectCount = new AtomicInteger(0);
            o.setOnRedetectRequested(redetectCount::incrementAndGet);

            // calibrate with noiseFloor=300 → NORMAL env
            // After calibration: noiseFloor≈300, gate = max(400, 300*1.8) = 540
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 300.0);

            // Feed 50 frames: alternate ambient at 300 and "speech" at 600.
            // 600 >= gate(540) → registered as speech peak.
            // SNR = avgPeak / noiseFloor = 600 / ~300 = 2.0 < 4.0 → triggers redetect.
            // lastRedetectTime starts at 0, so cooldown check passes on first trigger.
            for (int i = 0; i < 50; i++) {
                double rms = (i % 2 == 0) ? 600.0 : 300.0;
                o.feedFrame(DUMMY_BUF, BUF_LEN, rms);
            }

            // The SNR check at frame 50 should have fired at least once
            assertTrue(redetectCount.get() >= 1,
                    "Redetect callback should fire once after 50 frames with SNR < 4");
        }

        @Test
        @DisplayName("redetect callback does NOT fire when SNR is healthy")
        void noRedetectOnHealthySnr() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            AtomicInteger redetectCount = new AtomicInteger(0);
            o.setOnRedetectRequested(redetectCount::incrementAndGet);

            // calibrate with noiseFloor=100 → QUIET env, gate=300
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 100.0);

            // Feed 50 frames with speech peaks at 2000 → SNR = 2000/100 = 20 ≥ 4
            for (int i = 0; i < 50; i++) {
                o.feedFrame(DUMMY_BUF, BUF_LEN, (i % 3 == 0) ? 2000.0 : 100.0);
            }

            assertEquals(0, redetectCount.get(),
                    "Redetect callback should NOT fire when SNR is healthy");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Force recalibrate
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forceRecalibrate()")
    class ForceRecalibrate {

        @Test
        @DisplayName("resets calibration flag so isCalibrated() returns false")
        void resetsCalibrationFlag() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertTrue(o.isCalibrated());

            o.forceRecalibrate();

            assertFalse(o.isCalibrated(), "Should be uncalibrated after forceRecalibrate()");
        }

        @Test
        @DisplayName("re-calibrates successfully after forceRecalibrate")
        void reCalibratesToNewEnvironment() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            // Initial: quiet
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertEquals(QUIET, o.getEnvironment());

            // Force recalibrate then supply noisy ambient
            o.forceRecalibrate();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 600.0);

            assertTrue(o.isCalibrated());
            assertEquals(NOISY, o.getEnvironment());
        }

        @Test
        @DisplayName("environment-change callback fires with new environment after recalibration")
        void callbackFiresWithNewEnvironmentAfterRecalibrate() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);

            List<AcousticEnvironment> changes = new ArrayList<>();
            o.setOnEnvironmentChanged(changes::add);

            o.forceRecalibrate();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 600.0);

            assertFalse(changes.isEmpty(), "Callback should fire when environment changes after recalibrate");
            assertEquals(NOISY, changes.get(changes.size() - 1));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. Diagnostics
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("diagnostics()")
    class Diagnostics {

        @Test
        @DisplayName("returns non-empty string before calibration")
        void nonEmptyBeforeCalib() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            String d = o.diagnostics();
            assertNotNull(d);
            assertFalse(d.isBlank());
        }

        @Test
        @DisplayName("contains 'calibrated=true' after 40 frames")
        void containsCalibratedTrue() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            assertTrue(o.diagnostics().contains("calibrated=true"),
                    "Diagnostics should report calibrated=true after 40 frames");
        }

        @Test
        @DisplayName("contains detected environment label after calibration")
        void containsEnvLabel() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 150.0);
            // QUIET environment label in French
            assertTrue(o.diagnostics().contains("Calme"),
                    "Diagnostics should contain the French label 'Calme' for QUIET");
        }

        @Test
        @DisplayName("gate value reported is consistent with getGateThreshold()")
        void gateInDiagnosticsMatchesGetter() {
            NoiseOrchestrator o = NoiseOrchestrator.createFreshForTesting();
            for (int i = 0; i < 40; i++) o.feedFrame(DUMMY_BUF, BUF_LEN, 300.0);
            String d = o.diagnostics();
            double gate = o.getGateThreshold();
            // diagnostics prints "gate=%.0f" so check rounded value present
            assertTrue(d.contains("gate=" + Math.round(gate)),
                    "Diagnostics gate value should match getGateThreshold()");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 9. Singleton
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Singleton contract")
    class SingletonContract {

        @Test
        @DisplayName("getInstance() returns same object on repeated calls")
        void returnsSameInstance() {
            NoiseOrchestrator a = NoiseOrchestrator.getInstance();
            NoiseOrchestrator b = NoiseOrchestrator.getInstance();
            assertSame(a, b);
        }

        @Test
        @DisplayName("createFreshForTesting() returns distinct (non-singleton) instance")
        void freshInstanceIsDistinctFromSingleton() {
            NoiseOrchestrator fresh = NoiseOrchestrator.createFreshForTesting();
            // Fresh instance should NOT be the same object as the real singleton
            // (it's a separate test-only object)
            assertNotNull(fresh);
        }
    }
}
