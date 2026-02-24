package org.example.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AirSignatureService's pure-logic methods:
 *   - isSkin()          — RGB-rule skin detection
 *   - detectFingertip() — topmost skin-region centroid
 *
 * The start() / stop() methods require a webcam and the JavaFX toolkit,
 * so they are integration-tested separately.
 */
@DisplayName("AirSignatureService – Fingertip Detection")
class AirSignatureServiceTest {

    // ─── Pixel constants (ARGB) ───────────────────────────────────────────────
    /** Warm-tan skin pixel (R=200, G=140, B=100). Satisfies all RGB-rule conditions. */
    private static final int SKIN_TAN   = argb(200, 140, 100);

    /** Light-peach skin pixel (R=220, G=170, B=130). */
    private static final int SKIN_PEACH = argb(220, 170, 130);

    /** Dark-brown skin pixel (R=160, G=100, B=60). */
    private static final int SKIN_BROWN = argb(160, 100, 60);

    /** Non-skin: blue (R=20, G=50, B=200). r=20 fails r>95. */
    private static final int BACK_BLUE  = argb(20,  50,  200);

    /** Non-skin: green (R=30, G=200, B=60). r=30 fails r>95. */
    private static final int BACK_GREEN = argb(30,  200, 60);

    /** Non-skin: grey (R=128, G=128, B=128). |r-g|=0, fails >15 rule. */
    private static final int BACK_GREY  = argb(128, 128, 128);

    /** Non-skin: pure red (R=255, G=0, B=0). Fails g>40 requirement. */
    private static final int BACK_RED   = argb(255, 0,   0);

    private AirSignatureService svc;

    @BeforeEach
    void setUp() {
        svc = new AirSignatureService();
    }

    // =========================================================================
    // 1.  isSkin() — RGB heuristic
    // =========================================================================

    @Test
    @DisplayName("isSkin: warm-tan pixel satisfies all RGB rules → true")
    void isSkin_warmTan_returnsTrue() {
        assertTrue(svc.isSkin(SKIN_TAN),
            "R=200,G=140,B=100 should be detected as skin by the RGB rule");
    }

    @Test
    @DisplayName("isSkin: peach-tone pixel → true")
    void isSkin_peachTone_returnsTrue() {
        assertTrue(svc.isSkin(SKIN_PEACH),
            "R=220,G=170,B=130 should be detected as skin");
    }

    @Test
    @DisplayName("isSkin: dark-brown skin pixel → true")
    void isSkin_darkBrown_returnsTrue() {
        assertTrue(svc.isSkin(SKIN_BROWN),
            "R=160,G=100,B=60 should be detected as skin");
    }

    @Test
    @DisplayName("isSkin: blue background (r<95) → false")
    void isSkin_blue_returnsFalse() {
        assertFalse(svc.isSkin(BACK_BLUE),
            "Blue pixel fails r>95 rule");
    }

    @Test
    @DisplayName("isSkin: green background (r<95) → false")
    void isSkin_green_returnsFalse() {
        assertFalse(svc.isSkin(BACK_GREEN),
            "Green pixel fails r>95 rule");
    }

    @Test
    @DisplayName("isSkin: grey pixel (|r-g| == 0) → false")
    void isSkin_grey_returnsFalse() {
        assertFalse(svc.isSkin(BACK_GREY),
            "Grey pixel fails |r-g|>15 rule");
    }

    @Test
    @DisplayName("isSkin: pure red (g==0, fails g>40) → false")
    void isSkin_pureRed_returnsFalse() {
        assertFalse(svc.isSkin(BACK_RED),
            "Pure red fails g>40 rule");
    }

    // =========================================================================
    // 2.  detectFingertip() — topmost skin region
    // =========================================================================

    @Test
    @DisplayName("detectFingertip: all-background image → null (no fingertip)")
    void detectFingertip_noSkin_returnsNull() {
        BufferedImage img = blank(100, 80, BACK_BLUE);
        int[] tip = svc.detectFingertip(img, 100, 80);
        assertNull(tip, "No skin pixels → detectFingertip must return null");
    }

    @Test
    @DisplayName("detectFingertip: skin row present → returns [centroid_x, y]")
    void detectFingertip_skinRow_returnsCorrectY() {
        /*
         * Place 20 skin pixels in row y=15 (cols 0-39).
         * detectFingertip samples every 2nd pixel → 20 samples.
         * MIN_SKIN_ROW = 6, so 20 >= 6 → detected.
         * Expected y = 15.
         */
        BufferedImage img = blank(100, 80, BACK_BLUE);
        for (int x = 0; x < 40; x++) img.setRGB(x, 15, SKIN_TAN);
        int[] tip = svc.detectFingertip(img, 100, 80);
        assertNotNull(tip, "Row with skin pixels must be detected");
        assertEquals(15, tip[1], "Detected Y should match the skin row");
    }

    @Test
    @DisplayName("detectFingertip: skin row centroid x is computed correctly")
    void detectFingertip_skinRow_returnsCorrectX() {
        /*
         * Place skin pixels at cols 20-59 in row y=10.
         * Sampled pixels (step 2): 20, 22, 24, … 58 → 20 samples
         * sumX = 20+22+…+58 = 20 terms, avg ≈ 39.
         * The method returns sumX / skinCount.
         */
        BufferedImage img = blank(100, 80, BACK_BLUE);
        for (int x = 20; x < 60; x++) img.setRGB(x, 10, SKIN_TAN);
        int[] tip = svc.detectFingertip(img, 100, 80);
        assertNotNull(tip);
        int expectedAvg = (20 + 22 + 24 + 26 + 28 + 30 + 32 + 34 + 36 + 38
                         + 40 + 42 + 44 + 46 + 48 + 50 + 52 + 54 + 56 + 58) / 20;
        assertEquals(expectedAvg, tip[0],
            "Detected X should be the centroid of sampled skin pixels in the row");
    }

    @Test
    @DisplayName("detectFingertip: topmost row is returned when multiple skin rows exist")
    void detectFingertip_multipleSkinRows_returnsTopmost() {
        /*
         * Two bands of skin pixels: row y=5 and row y=40.
         * detectFingertip scans top-to-bottom → row 5 must be returned.
         */
        BufferedImage img = blank(100, 80, BACK_BLUE);
        for (int x = 0; x < 40; x++) img.setRGB(x, 5,  SKIN_TAN);
        for (int x = 0; x < 40; x++) img.setRGB(x, 40, SKIN_TAN);
        int[] tip = svc.detectFingertip(img, 100, 80);
        assertNotNull(tip);
        assertEquals(5, tip[1], "Should return the topmost (y=5) skin row, not y=40");
    }

    @Test
    @DisplayName("detectFingertip: thin skin band (< MIN_SKIN_ROW samples) is skipped")
    void detectFingertip_tooFewSkinPixels_skipsRow() {
        /*
         * Place only 10 skin pixels in row y=3 (cols 0-9).
         * Sampling every 2nd pixel gives 5 samples → below MIN_SKIN_ROW=6 → skipped.
         * Place a qualifying band at row y=30 (14 skin pixels → 7 samples ≥ 6).
         */
        BufferedImage img = blank(100, 80, BACK_BLUE);
        for (int x = 0; x < 10; x++) img.setRGB(x, 3,  SKIN_TAN);  // 5 samples — skipped
        for (int x = 0; x < 14; x++) img.setRGB(x, 30, SKIN_TAN);  // 7 samples — detected
        int[] tip = svc.detectFingertip(img, 100, 80);
        assertNotNull(tip, "Qualifying row at y=30 must be detected");
        assertEquals(30, tip[1], "Thin row y=3 should be skipped; y=30 should be returned");
    }

    @Test
    @DisplayName("detectFingertip: service starts not running")
    void service_initialState_notRunning() {
        assertFalse(svc.isRunning(),
            "AirSignatureService must not be running before start() is called");
    }

    @Test
    @DisplayName("stop() is idempotent when never started")
    void stop_whenNeverStarted_doesNotThrow() {
        assertDoesNotThrow(svc::stop,
            "Calling stop() before start() must not throw any exception");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static int argb(int r, int g, int b) {
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static BufferedImage blank(int w, int h, int color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, color);
        return img;
    }
}
