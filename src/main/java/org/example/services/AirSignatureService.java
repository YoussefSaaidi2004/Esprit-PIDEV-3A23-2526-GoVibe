package org.example.services;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Captures webcam frames and detects the fingertip position using
 * a simple RGB skin-colour rule.  Uses the shared pre-warmed webcam
 * from WebcamManager and the bulk toFXImage() for efficient frame copy.
 *
 * Frame rate: 15 fps (sufficient for smooth air-signature tracing).
 */
public class AirSignatureService {

    private final AtomicBoolean running = new AtomicBoolean(false);

    // Frame interval: 66 ms = ~15 fps
    private static final long FRAME_MS = 66;

    /**
     * Start capturing.  Callbacks are invoked on the JavaFX thread.
     *
     * @param onFrame      receives each camera frame as a WritableImage (mirror-flipped)
     * @param onFingertip  receives normalised (x, y) in [0,1] when a fingertip is found;
     *                     receives (-1, -1) when nothing is detected;
     *                     receives (-2, -2) when no camera is available
     */
    public void start(Consumer<WritableImage> onFrame,
                      BiConsumer<Double, Double> onFingertip) {

        if (running.get()) return;
        running.set(true);

        Thread t = new Thread(() -> {
            // Borrow the shared pre-warmed webcam
            Webcam webcam = WebcamManager.borrow(4000);
            if (webcam == null) {
                Platform.runLater(() -> onFingertip.accept(-2.0, -2.0));
                running.set(false);
                return;
            }

            while (running.get() && webcam.isOpen()) {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame == null) { Thread.sleep(30); continue; }

                    int w = frame.getWidth();
                    int h = frame.getHeight();

                    int[] tip = detectFingertip(frame, w, h);

                    // Bulk bulk pixel copy via WebcamManager
                    WritableImage fxImg = WebcamManager.toFXImage(frame, true);

                    double tipX = tip == null ? -1.0 : (double) (w - tip[0]) / w;
                    double tipY = tip == null ? -1.0 : (double) tip[1] / h;

                    Platform.runLater(() -> {
                        onFrame.accept(fxImg);
                        onFingertip.accept(tipX, tipY);
                    });

                    Thread.sleep(FRAME_MS);
                } catch (InterruptedException ie) {
                    break;
                } catch (Exception ex) {
                    System.err.println("[AirSig] Frame error: " + ex.getMessage());
                }
            }
            WebcamManager.release(); // keep camera open for next session
        });
        t.setDaemon(true);
        t.setName("air-sig-capture");
        t.start();
    }

    /** Stop capturing.  The shared webcam stays open for the next session. */
    public void stop() {
        running.set(false);
    }

    public boolean isRunning() { return running.get(); }

    // ─── Fingertip detection ────────────────────────────────────────────────

    /**
     * Finds the topmost cluster of skin-coloured pixels.
     * Returns the centroid of the topmost horizontal band that has
     * at least MIN_SKIN_PIXELS consecutive skin pixels, or null if nothing found.
     */
    /* package-private for unit testing */
    int[] detectFingertip(BufferedImage img, int w, int h) {
        final int MIN_SKIN_ROW = 6; // minimum skin pixels in a row to count

        for (int y = 0; y < h; y++) {
            int skinCount = 0;
            int sumX = 0;
            for (int x = 0; x < w; x += 2) { // sample every 2nd pixel for speed
                if (isSkin(img.getRGB(x, y))) {
                    skinCount++;
                    sumX += x;
                }
            }
            if (skinCount >= MIN_SKIN_ROW) {
                return new int[]{sumX / skinCount, y};
            }
        }
        return null;
    }

    /**
     * Simple RGB skin-colour rule tuned for indoor lighting.
     */
    /* package-private for unit testing */
    boolean isSkin(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b =  rgb        & 0xFF;
        return r > 95
            && g > 40 && b > 20
            && r > g  && r > b
            && Math.abs(r - g) > 15
            && (r - b) > 15
            && r > 100;
    }

    // ─── BufferedImage → WritableImage (delegated to WebcamManager) ───────────────
    // (kept for backward compatibility with unit tests that call it directly)
    private WritableImage toFXImage(BufferedImage img, int w, int h, boolean mirror) {
        return WebcamManager.toFXImage(img, mirror);
    }
}
