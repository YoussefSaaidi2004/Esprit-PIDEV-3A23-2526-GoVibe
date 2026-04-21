package org.example.assistant;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Intelligent Adaptive Noise Suppression Orchestrator for GoVibe Voice Assistant.
 *
 * <p>Implements a closed-loop, three-stage acoustic pipeline:
 *
 * <pre>
 *   Raw Audio
 *      │
 *      ▼
 *  ┌──────────┐     environment profile     ┌─────────────────────┐
 *  │ DETECTOR │────────────────────────────▶│    ORCHESTRATOR     │
 *  │  (BERP-  │                             │  selects gate params │
 *  │ inspired)│◀─── re-detect trigger ──────│  from profile map   │
 *  └──────────┘                             └────────┬────────────┘
 *                                                    │ gate threshold
 *      ┌───────────────────────────────────────────◀─┘
 *      ▼
 *  ┌──────────────────────────────────────────┐
 *  │  ADAPTIVE FILTER  (LMS-inspired gate)    │
 *  │  isSpeech(frame) + feed-all-to-Vosk      │
 *  └──────────────────┬───────────────────────┘
 *                     │ clean speech / dispatched result
 *                     ▼
 *             ┌──────────────┐
 *             │  VALIDATOR   │  monitors output SNR
 *             │  (SONIC-like)│  triggers re-detect if SNR drops
 *             └──────────────┘
 * </pre>
 *
 * <h3>Acoustic Environments</h3>
 * <table>
 *   <tr><th>Environment</th><th>Noise Floor RMS</th><th>Gate Mult.</th></tr>
 *   <tr><td>SILENT</td><td>&lt; 80</td><td>1.2×</td></tr>
 *   <tr><td>QUIET</td><td>80 – 200</td><td>1.5×</td></tr>
 *   <tr><td>NORMAL</td><td>200 – 450</td><td>1.8×</td></tr>
 *   <tr><td>NOISY</td><td>450 – 800</td><td>2.4×</td></tr>
 *   <tr><td>VERY_NOISY</td><td>&gt; 800</td><td>3.2×</td></tr>
 * </table>
 */
public class NoiseOrchestrator {

    // ── Acoustic Environment profiles ─────────────────────────────────────────
    public enum AcousticEnvironment {
        SILENT   ("Silencieux",   80,   -1,               1.2,  200),
        QUIET    ("Calme",       200,   80,               1.5,  300),
        NORMAL   ("Normal",      450,   200,              1.8,  400),
        NOISY    ("Bruyant",     800,   450,              2.4,  550),
        VERY_NOISY("Très bruyant", Double.MAX_VALUE, 800, 3.2, 700);

        /** Human-readable French label for TTS announcements. */
        public final String label;
        /** Gate multiplier: threshold = noiseFloor × multiplier. */
        public final double multiplier;
        /** Minimum gate RMS (floor so faint speech still passes). */
        public final double minGate;
        /** Upper floor bound for this environment (noiseFloor ≤ this). */
        final double maxFloor;
        /** Lower floor bound for this environment (noiseFloor > this). */
        final double minFloor;

        AcousticEnvironment(String label, double maxFloor, double minFloor,
                            double multiplier, double minGate) {
            this.label      = label;
            this.maxFloor   = maxFloor;
            this.minFloor   = minFloor;
            this.multiplier = multiplier;
            this.minGate    = minGate;
        }

        /** Resolve environment from a measured ambient noise floor RMS. */
        public static AcousticEnvironment fromFloor(double floor) {
            for (AcousticEnvironment e : values()) {
                if (floor >= e.minFloor && floor < e.maxFloor) return e;
            }
            return VERY_NOISY;
        }

        /** Compute the adaptive gate threshold for this environment profile. */
        public double gateThreshold(double floor) {
            return Math.max(minGate, floor * multiplier);
        }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile NoiseOrchestrator instance;

    public static NoiseOrchestrator getInstance() {
        if (instance == null) {
            synchronized (NoiseOrchestrator.class) {
                if (instance == null) instance = new NoiseOrchestrator();
            }
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Current detected environment (updated by Detector). */
    private final AtomicReference<AcousticEnvironment> currentEnv =
            new AtomicReference<>(AcousticEnvironment.NORMAL);

    /** Current ambient noise floor RMS (rolling average). */
    private volatile double noiseFloor    = 0;

    /** Current adaptive gate threshold (used by VoiceAssistantService). */
    private volatile double gateThreshold = 500;

    /** True once initial calibration has completed. */
    private volatile boolean calibrated   = false;

    // ── Detector state — sliding window for BERP-inspired environment sensing ─
    private static final int DETECTOR_WINDOW   = 80;  // ~5 s at 4096-byte frames (~16ms each)
    private static final int CALIB_FRAMES      = 40;  // frames to build initial floor estimate
    private static final long TTS_SETTLE_MS    = 4_500; // discard first ~4.5 s (TTS bleed)

    private final Deque<Double> rmsWindow   = new ArrayDeque<>(DETECTOR_WINDOW + 10);
    private final Deque<Double> speechPeaks = new ArrayDeque<>(20); // speech-frame peaks

    private int  calibCount     = 0;
    private double calibSum     = 0;
    private long  startTime     = 0;
    private boolean discarding  = false; // true during TTS settle phase

    // ── Validator state — SONIC-inspired SNR monitor ───────────────────────────
    private static final double SNR_WARN_THRESHOLD   = 4.0;  // speech/noise ratio below = warn
    private static final int    SNR_SAMPLE_INTERVAL  = 50;   // check SNR every N frames
    private static final long   REDETECT_COOLDOWN_MS = 15_000; // min ms between re-detects
    private int  snrSampleCount  = 0;
    private long lastRedetectTime = 0;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    /** Called when the environment changes. Arg = new environment. */
    private volatile Consumer<AcousticEnvironment> onEnvironmentChanged;

    /** Called when the validator detects degraded SNR and triggers re-calibration. */
    private volatile Runnable onRedetectRequested;

    private NoiseOrchestrator() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Package-private constructor for unit tests only.
     * Accepts a custom TTS settle duration so tests complete instantly.
     *
     * @param ttsSettleMs milliseconds to skip at startup (pass 0 in tests)
     */
    NoiseOrchestrator(long ttsSettleMs) {
        // Back-date startTime so the settle phase is already "expired" at construction.
        startTime = System.currentTimeMillis() - ttsSettleMs;
    }

    /**
     * Create a fresh, isolated instance for unit testing.
     * Does NOT replace the global singleton — safe to call in @BeforeEach.
     */
    static NoiseOrchestrator createFreshForTesting() {
        return new NoiseOrchestrator(TTS_SETTLE_MS); // settle = already expired
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Register callback invoked when the detected environment changes. */
    public void setOnEnvironmentChanged(Consumer<AcousticEnvironment> cb) {
        this.onEnvironmentChanged = cb;
    }

    /** Register callback invoked when the validator requests environment re-detection. */
    public void setOnRedetectRequested(Runnable cb) {
        this.onRedetectRequested = cb;
    }

    /** Current adaptive gate threshold — call from the audio capture loop. */
    public double getGateThreshold() { return gateThreshold; }

    /** Whether initial calibration is done and the gate is reliable. */
    public boolean isCalibrated() { return calibrated; }

    /** Current detected acoustic environment. */
    public AcousticEnvironment getEnvironment() { return currentEnv.get(); }

    /** Current noise floor RMS (rolling average of ambient frames). */
    public double getNoiseFloor() { return noiseFloor; }

    /**
     * Feed one audio frame to the orchestrator.  Must be called for every frame
     * read from the microphone, regardless of RMS level.
     *
     * <p>Returns {@code true} if this frame counts as "speech" (RMS above gate).
     * The caller should feed ALL frames to Vosk; use this return value only to
     * track the last-speech timestamp for dispatch gating.
     *
     * @param buffer raw 16-bit LE PCM bytes
     * @param n      valid bytes in buffer (may be &lt; buffer.length)
     * @param rms    pre-computed RMS energy for this frame
     */
    public boolean feedFrame(byte[] buffer, int n, double rms) {
        long now = System.currentTimeMillis();

        // ── TTS settle phase: discard initial frames with speaker bleed ────────
        if (startTime == 0) startTime = now;
        if ((now - startTime) < TTS_SETTLE_MS) {
            discarding = true;
            return false; // treat as silence during settle
        }

        if (discarding) {
            // Transition out of settle phase: re-zero calibration state
            discarding   = false;
            calibCount   = 0;
            calibSum     = 0;
            rmsWindow.clear();
            System.out.println("[NoiseOrchestrator] TTS settle complete — starting calibration.");
        }

        // ── Phase 1: Initial calibration ──────────────────────────────────────
        if (!calibrated) {
            calibSum += rms;
            calibCount++;

            if (calibCount >= CALIB_FRAMES) {
                double floor = calibSum / calibCount;
                commitNewFloor(floor, now);
                calibrated = true;
            }
            return false; // don't dispatch during calibration
        }

        // ── Phase 2: Continuous environment detection (BERP-inspired) ─────────
        // Maintain a sliding window of recent RMS values.
        // Separate ambient frames (below gate) from speech frames (above gate).
        if (rmsWindow.size() >= DETECTOR_WINDOW) rmsWindow.pollFirst();
        rmsWindow.addLast(rms);

        boolean isSpeech = (rms >= gateThreshold);

        if (!isSpeech) {
            // Ambient frame — update rolling noise floor using EMA
            noiseFloor = 0.98 * noiseFloor + 0.02 * rms;
        } else {
            // Speech frame — track peaks for SNR estimation
            if (speechPeaks.size() >= 20) speechPeaks.pollFirst();
            speechPeaks.addLast(rms);
        }

        // ── Environment change detection ───────────────────────────────────────
        // Every DETECTOR_WINDOW frames, re-classify the environment from the
        // current rolling noise floor and update the gate if the profile changed.
        if (rmsWindow.size() == DETECTOR_WINDOW && (rmsWindow.size() % 20 == 0)) {
            double rollingFloor = rmsWindow.stream()
                    .sorted()
                    .limit(DETECTOR_WINDOW / 4L)  // lowest 25% = ambient noise
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(noiseFloor);
            checkEnvironmentChange(rollingFloor, now);
        }

        // ── Phase 3: SONIC-inspired SNR validation ─────────────────────────────
        snrSampleCount++;
        if (snrSampleCount >= SNR_SAMPLE_INTERVAL) {
            snrSampleCount = 0;
            validateSnr(now);
        }

        return isSpeech;
    }

    /**
     * Force an immediate re-calibration — called when the user manually
     * triggers it (e.g. voice command "recalibrer") or after a long pause.
     */
    public void forceRecalibrate() {
        System.out.println("[NoiseOrchestrator] Forced recalibration requested.");
        calibrated   = false;
        calibCount   = 0;
        calibSum     = 0;
        startTime    = System.currentTimeMillis() - TTS_SETTLE_MS; // skip settle
        rmsWindow.clear();
        speechPeaks.clear();
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /** Apply a newly measured noise floor: compute gate, classify environment, notify. */
    private void commitNewFloor(double floor, long now) {
        noiseFloor = floor;
        AcousticEnvironment newEnv = AcousticEnvironment.fromFloor(floor);
        double newGate = newEnv.gateThreshold(floor);

        AcousticEnvironment prev = currentEnv.getAndSet(newEnv);
        gateThreshold = newGate;

        System.out.printf(
                "[NoiseOrchestrator] \uD83C\uDF9A Environment: %-10s | floor=%.0f | gate=%.0f | mult=%.1f%n",
                newEnv.label, floor, newGate, newEnv.multiplier);

        if (prev != newEnv) {
            Consumer<AcousticEnvironment> cb = onEnvironmentChanged;
            if (cb != null) cb.accept(newEnv);
        }
    }

    /** Check if the rolling noise floor has drifted into a different environment profile. */
    private void checkEnvironmentChange(double rollingFloor, long now) {
        AcousticEnvironment current = currentEnv.get();
        AcousticEnvironment detected = AcousticEnvironment.fromFloor(rollingFloor);

        if (detected != current) {
            System.out.printf(
                    "[NoiseOrchestrator] \uD83D\uDD04 Environment shift: %s → %s (floor %.0f→%.0f)%n",
                    current.label, detected.label, noiseFloor, rollingFloor);
            commitNewFloor(rollingFloor, now);
        }
    }

    /**
     * Validator: estimate current SNR from speech peaks vs noise floor.
     * If SNR drops below threshold, request re-detection from the orchestrator.
     */
    private void validateSnr(long now) {
        if (speechPeaks.isEmpty()) return;

        double avgPeak = speechPeaks.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double snr = (noiseFloor > 0) ? avgPeak / noiseFloor : 99;

        if (snr < SNR_WARN_THRESHOLD && (now - lastRedetectTime) > REDETECT_COOLDOWN_MS) {
            lastRedetectTime = now;
            System.out.printf(
                    "[NoiseOrchestrator] \u26A0\uFE0F SNR degraded (%.1f < %.1f) — triggering re-detection.%n",
                    snr, SNR_WARN_THRESHOLD);
            // Re-detect: collect fresh ambient samples to recompute noise floor
            triggerRedetect(now);
        }
    }

    /**
     * Trigger a soft re-detection — collect a fresh window of ambient frames
     * and recompute the noise floor without a full restart.
     */
    private void triggerRedetect(long now) {
        // Compute updated floor from the ambient (below-gate) portion of the window
        if (rmsWindow.isEmpty()) return;

        double[] arr = rmsWindow.stream().mapToDouble(Double::doubleValue).toArray();
        // Sort and take lowest 30% as ambient estimate
        java.util.Arrays.sort(arr);
        int cutoff = Math.max(1, arr.length * 3 / 10);
        double sum = 0;
        for (int i = 0; i < cutoff; i++) sum += arr[i];
        double freshFloor = sum / cutoff;

        System.out.printf("[NoiseOrchestrator] Re-detection floor estimate: %.0f%n", freshFloor);
        checkEnvironmentChange(freshFloor, now);
        speechPeaks.clear(); // reset SNR tracking

        Runnable cb = onRedetectRequested;
        if (cb != null) cb.run();
    }

    // ── Diagnostics ───────────────────────────────────────────────────────────

    /** Returns a one-line diagnostic string suitable for logging. */
    public String diagnostics() {
        return String.format(
                "env=%-10s  floor=%.0f  gate=%.0f  snrPeaks=%d  calibrated=%b",
                currentEnv.get().label, noiseFloor, gateThreshold,
                speechPeaks.size(), calibrated);
    }
}
