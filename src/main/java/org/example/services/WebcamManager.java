package org.example.services;

import com.github.sarxos.webcam.Webcam;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * Singleton webcam manager.
 *
 * Responsibilities:
 *  1. Pre-open the camera once at dashboard startup (prewarm).
 *  2. Let GestureRecognitionService and AirSignatureService borrow
 *     the already-open camera — eliminating the 1-3s cold-start delay
 *     from every gesture/signature session.
 *  3. Provide a bulk-copy toFXImage() used by both services —
 *     one getRGB row-array call + one setPixels bulk write per frame,
 *     instead of 307 200 individual pixel operations per frame.
 *
 * Camera resolution is 320×240.  That is sufficient for both gesture
 * recognition and air-signature fingertip tracking, and is 4× cheaper
 * to copy than 640×480.
 */
public final class WebcamManager {

    public static final int CAM_W = 320;
    public static final int CAM_H = 240;

    private static volatile Webcam sharedCam  = null;
    private static volatile boolean prewarming = false;

    private WebcamManager() {}

    // ── Pre-warm ──────────────────────────────────────────────────────────────

    /**
     * Open the webcam in the background.  Safe to call multiple times.
     * Should be called right after the user logs in so the camera is ready
     * by the time the payment flow reaches the gesture screen.
     */
    public static synchronized void prewarm() {
        if ((sharedCam != null && sharedCam.isOpen()) || prewarming) return;
        prewarming = true;
        Thread t = new Thread(() -> {
            try {
                Webcam cam = Webcam.getDefault();
                if (cam == null) {
                    System.err.println("[WebcamManager] No camera found.");
                    return;
                }
                cam.setCustomViewSizes(new Dimension(CAM_W, CAM_H));
                cam.setViewSize(new Dimension(CAM_W, CAM_H));
                cam.open();
                sharedCam = cam;

                // Prime the JIT: run one toFXImage so WritableImage / PixelWriter
                // classes are loaded and the hot loop is compiled before first use.
                BufferedImage warm = cam.getImage();
                if (warm != null) toFXImage(warm, true);

                System.out.println("[WebcamManager] Camera pre-warmed at "
                    + CAM_W + "x" + CAM_H + " — gesture screen will open instantly.");
            } catch (Exception e) {
                System.err.println("[WebcamManager] Prewarm error: " + e.getMessage());
            } finally {
                prewarming = false;
            }
        }, "webcam-prewarm");
        t.setDaemon(true);
        t.start();
    }

    // ── Borrow / release ─────────────────────────────────────────────────────

    /**
     * Borrow the shared webcam, waiting up to {@code timeoutMs} for it to be ready.
     * Returns {@code null} if no camera is available.
     *
     * <p>Callers must NOT close the returned Webcam.  Call {@link #release} instead.
     */
    public static Webcam borrow(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (sharedCam != null && sharedCam.isOpen()) return sharedCam;
            try { Thread.sleep(50); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // If pre-warming failed or hasn't started, try opening a fresh camera.
        if (sharedCam == null || !sharedCam.isOpen()) {
            try {
                Webcam cam = Webcam.getDefault();
                if (cam != null) {
                    cam.setCustomViewSizes(new Dimension(CAM_W, CAM_H));
                    cam.setViewSize(new Dimension(CAM_W, CAM_H));
                    cam.open();
                    sharedCam = cam;
                }
            } catch (Exception e) {
                System.err.println("[WebcamManager] Fallback open error: " + e.getMessage());
            }
        }
        return sharedCam;
    }

    /**
     * Signal that a service has finished using the webcam.
     * The camera stays open for the next session.
     */
    public static void release() {
        // Intentionally do nothing — camera stays open for reuse.
        // The JVM shutdown will close the daemon thread's camera automatically.
    }

    // ── Bulk frame conversion ─────────────────────────────────────────────────

    /**
     * Convert a {@link BufferedImage} to a JavaFX {@link WritableImage} using
     * a single bulk {@code setPixels} call per row — ~40× faster than individual
     * {@code setArgb} loops.
     *
     * @param img    source frame from the webcam
     * @param mirror true for selfie-view (flip horizontally)
     */
    public static WritableImage toFXImage(BufferedImage img, boolean mirror) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        int[] row    = new int[w];

        for (int y = 0; y < h; y++) {
            // Single bulk read for the entire row — much faster than per-pixel getRGB
            img.getRGB(0, y, w, 1, row, 0, w);
            int base = y * w;
            if (mirror) {
                for (int x = 0; x < w; x++)
                    pixels[base + x] = row[w - 1 - x] | 0xFF000000;
            } else {
                for (int x = 0; x < w; x++)
                    pixels[base + x] = row[x] | 0xFF000000;
            }
        }

        WritableImage fxImg = new WritableImage(w, h);
        fxImg.getPixelWriter().setPixels(
            0, 0, w, h,
            PixelFormat.getIntArgbInstance(),
            pixels, 0, w
        );
        return fxImg;
    }
}
