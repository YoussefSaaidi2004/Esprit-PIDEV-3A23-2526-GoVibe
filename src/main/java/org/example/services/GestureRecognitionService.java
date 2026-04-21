package org.example.services;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * AI Hand-Gesture Recognition Service
 *
 * Algorithm:
 *  1. Capture frame from webcam (640×480).
 *  2. Downsample to 160×90 for fast HSV-based skin segmentation.
 *  3. Compute bounding box of the largest skin region.
 *  4. Build a column histogram over the TOP 40% of that bounding box
 *     to detect "finger peaks" projecting upward.
 *  5. Classify gesture from peak count + bounding-box aspect ratio + fill ratio.
 *
 * Gestures detected: NONE, THUMBS_UP, OPEN_PALM, FIST, POINTING
 *
 * All callbacks are delivered on the JavaFX Application Thread.
 */
public class GestureRecognitionService {

    // ── Gesture enum ─────────────────────────────────────────────────────────
    public enum Gesture {
        NONE,
        THUMBS_UP,   // 1 finger (thumb) extended, tall aspect ratio
        POINTING,    // 1 finger (index) extended
        FIST,        // compact blob, ≤1 finger peak
        OPEN_PALM    // 4-5 finger peaks
    }

    // ── Processing resolution ─────────────────────────────────────────────────
    // 160×90 is 1/4 of 640×360 — fast enough for 30fps on a CPU
    private static final int PW = 160;
    private static final int PH = 90;

    // ── State ─────────────────────────────────────────────────────────────────
    private volatile boolean running = false;
    private Thread workerThread;
    private Webcam webcam;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Start recognition.
     * @param onFrame     receives the mirrored camera frame for display (~30 fps)
     * @param onGesture   receives the detected gesture (~30 fps)
     */
    public void start(Consumer<WritableImage> onFrame, Consumer<Gesture> onGesture) {
        start(onFrame, onGesture, null);
    }

    /**
     * Start recognition with optional AI-relay callback.
     * @param onFrame     receives the mirrored camera frame for display (~30 fps)
     * @param onGesture   receives the detected gesture (~30 fps)
     * @param onAiRelay   receives gesture name strings for dispatch to the Python agent
     *                    (deduplicated — only fires on gesture change); may be null
     */
    public void start(Consumer<WritableImage> onFrame,
                      Consumer<Gesture>       onGesture,
                      Consumer<String>        onAiRelay) {
        if (running) return;
        running = true;

        workerThread = new Thread(() -> {
            try {
                // Borrow the shared pre-warmed camera from WebcamManager.
                // This avoids the "Cannot change resolution when webcam is open" error
                // that occurs when GestureRecognitionService tries to reopen the already-
                // open camera at a different resolution (640×480 vs the pre-warmed 320×240).
                webcam = WebcamManager.borrow(4000);
                if (webcam == null) {
                    Platform.runLater(() -> onGesture.accept(Gesture.NONE));
                    return;
                }

                Gesture lastRelayed = Gesture.NONE;

                while (running) {
                    BufferedImage raw = webcam.getImage();
                    if (raw == null) { Thread.sleep(50); continue; }

                    int rw = raw.getWidth();
                    int rh = raw.getHeight();

                    // Mirror for display (selfie-view)
                    WritableImage fxFrame = toFXImage(raw, rw, rh, true);
                    Gesture g = classify(raw, rw, rh);

                    // AI relay: send gesture name to agent on gesture change
                    final Gesture relayG = g;
                    if (onAiRelay != null && g != lastRelayed) {
                        lastRelayed = g;
                        if (g != Gesture.NONE) {
                            onAiRelay.accept(g.name());
                        }
                    }

                    Platform.runLater(() -> {
                        onFrame.accept(fxFrame);
                        onGesture.accept(relayG);
                    });

                    Thread.sleep(33); // ~30 fps
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[GestureRecognition] " + e.getMessage());
            } finally {
                WebcamManager.release(); // return shared camera
            }
        }, "gesture-ai-thread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /** Stop recognition and return camera to WebcamManager shared pool. */
    public void stop() {
        running = false;
        if (workerThread != null) workerThread.interrupt();
        // Webcam is managed by WebcamManager — do not close it here;
        // WebcamManager.release() in the worker thread's finally block handles it.
    }

    // ── Core AI classification ────────────────────────────────────────────────

    /* package-private for unit testing */
    Gesture classify(BufferedImage raw, int rw, int rh) {
        // 1. Downsample + skin mask (mirrored to match display)
        boolean[][] skin = new boolean[PH][PW];
        int skinCount = 0;
        float xScale = (float) rw / PW;
        float yScale = (float) rh / PH;

        for (int py = 0; py < PH; py++) {
            for (int px = 0; px < PW; px++) {
                int srcX = Math.min((int)(px * xScale), rw - 1);
                int srcY = Math.min((int)(py * yScale), rh - 1);
                int rgb  = raw.getRGB(srcX, srcY);
                int mirroredPx = PW - 1 - px;   // mirror consistent with display
                skin[py][mirroredPx] = isSkin(rgb);
                if (skin[py][mirroredPx]) skinCount++;
            }
        }

        // Not enough skin → no hand visible
        if (skinCount < 80) return Gesture.NONE;

        // 2. Bounding box of all skin pixels
        int minX = PW, maxX = 0, minY = PH, maxY = 0;
        for (int py = 0; py < PH; py++) {
            for (int px = 0; px < PW; px++) {
                if (skin[py][px]) {
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
            }
        }

        int bboxW = maxX - minX + 1;
        int bboxH = maxY - minY + 1;
        if (bboxW < 5 || bboxH < 5) return Gesture.NONE;

        float aspectRatio = (float) bboxH / bboxW;   // > 1 means taller than wide
        float fillRatio   = (float) skinCount / (bboxW * bboxH);

        // 3. Column histogram in the TOP 40% of the hand bounding box
        //    (where fingers would project upward)
        int topBoundary = minY + (int)(bboxH * 0.40f);
        int[] colHist = new int[bboxW];

        for (int py = minY; py < topBoundary && py < PH; py++) {
            for (int px = minX; px <= maxX && px < PW; px++) {
                if (skin[py][px]) colHist[px - minX]++;
            }
        }

        // 4. Smooth the histogram to reduce noise
        int[] smoothed = smooth(colHist, bboxW);

        // 5. Count peaks (fingers) above 20% of max
        int maxH = 0;
        for (int v : smoothed) if (v > maxH) maxH = v;
        int threshold = Math.max(1, maxH / 5);
        int minSep    = Math.max(2, bboxW / 10);
        int fingerCount = countPeaks(smoothed, bboxW, threshold, minSep);

        // 6. Classify ─────────────────────────────────────────────────────────
        //
        //   THUMBS_UP : 1 narrow finger peak, tall aspect, moderate fill
        //               (thumb sticks up, rest of hand is curled inward)
        //   POINTING  : 1 finger peak, aspect ratio ≥ 1.0
        //   OPEN_PALM : 4-5 finger peaks spread across full width
        //   FIST      : compact blob, high fill, few peaks
        //   NONE      : ambiguous / too small

        if (fingerCount == 1) {
            if (aspectRatio > 1.2f && fillRatio < 0.75f) {
                return Gesture.THUMBS_UP;
            }
            if (aspectRatio >= 0.9f) {
                return Gesture.POINTING;
            }
        }

        if (fingerCount >= 4) {
            return Gesture.OPEN_PALM;
        }

        if (fillRatio > 0.55f && fingerCount <= 1) {
            return Gesture.FIST;
        }

        return Gesture.NONE;
    }

    // ── HSV-based skin detection ──────────────────────────────────────────────
    //
    // Much more robust than pure RGB — resists lighting changes.
    // Skin HSV range (hue 0-25° or 335-360°, sat 18-72%, brightness >30%)
    /* package-private for unit testing */
    static boolean isSkin(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b =  rgb        & 0xFF;

        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        float h = hsb[0]; // [0, 1]
        float s = hsb[1];
        float v = hsb[2];

        boolean hueOk = (h < 0.072f || h > 0.930f); // 0-26° or 335-360°
        boolean satOk = (s > 0.18f  && s < 0.72f);
        boolean valOk = (v > 0.30f);

        return hueOk && satOk && valOk;
    }

    // ── Signal-processing helpers ─────────────────────────────────────────────

    /** Gaussian-like smoothing with kernel radius 3. */
    /* package-private for unit testing */
    static int[] smooth(int[] hist, int len) {
        int[] out = new int[len];
        int kernelRadius = 3;
        for (int i = 0; i < len; i++) {
            int sum = 0, cnt = 0;
            for (int k = i - kernelRadius; k <= i + kernelRadius; k++) {
                if (k >= 0 && k < len) { sum += hist[k]; cnt++; }
            }
            out[i] = (cnt > 0) ? sum / cnt : 0;
        }
        return out;
    }

    /**
     * Count peaks in a 1-D histogram.
     * @param hist      smoothed histogram values
     * @param len       histogram length
     * @param threshold values below this are considered valley
     * @param minSep    minimum column separation between two distinct peaks
     */
    /* package-private for unit testing */
    static int countPeaks(int[] hist, int len, int threshold, int minSep) {
        int peaks = 0;
        boolean inPeak = false;
        int lastPeakIdx = -1000;

        for (int i = 0; i < len; i++) {
            if (hist[i] > threshold && !inPeak) {
                if (i - lastPeakIdx > minSep) {
                    peaks++;
                    lastPeakIdx = i;
                }
                inPeak = true;
            } else if (hist[i] <= threshold) {
                inPeak = false;
            }
        }
        return peaks;
    }

    /** Convert BufferedImage to JavaFX WritableImage via WebcamManager bulk path. */
    private static WritableImage toFXImage(BufferedImage img, int w, int h, boolean mirror) {
        return WebcamManager.toFXImage(img, mirror);
    }
}
