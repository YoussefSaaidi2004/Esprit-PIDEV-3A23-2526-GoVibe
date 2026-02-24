package org.example.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the GestureRecognitionService AI pipeline.
 *
 * Tests cover four layers of the algorithm:
 *   1. isSkin()         - HSV skin segmentation
 *   2. smooth()         - Gaussian histogram smoothing
 *   3. countPeaks()     - Finger-peak counting
 *   4. classify()       - End-to-end gesture classification with synthetic images
 *
 * All tests are pure-Java (no JavaFX toolkit required).
 */
@DisplayName("GestureRecognitionService – AI Gesture Pipeline")
class GestureRecognitionServiceTest {

    // ─── Pixel constants ──────────────────────────────────────────────────────
    /**
     * Warm-tan skin pixel (R=200, G=140, B=100).
     * HSV: H≈24° (0.067 in [0,1]), S=0.50, V=0.78  → all skin criteria met.
     */
    private static final int SKIN_TAN    = argb(200, 140, 100);

    /**
     * Light-peachy skin pixel (R=205, G=140, B=100).
     * HSV: H=22.9° (0.0635), S=0.512, V=0.804 → all skin criteria met.
     */
    private static final int SKIN_PEACH  = argb(205, 140, 100);

    /**
     * Darker brown skin pixel (R=160, G=100, B=60).
     * HSV: H≈21°, S=0.625, V=0.627 → skin.
     */
    private static final int SKIN_BROWN  = argb(160, 100, 60);

    /** Non-skin: saturated blue (R=20, G=50, B=200). */
    private static final int BACK_BLUE   = argb(20, 50, 200);

    /** Non-skin: vivid green (R=30, G=200, B=60). */
    private static final int BACK_GREEN  = argb(30, 200, 60);

    /** Non-skin: near-black (R=10, G=10, B=10). Too dark → V < 0.30. */
    private static final int BACK_BLACK  = argb(10, 10, 10);

    /** Non-skin: white-grey (R=220, G=220, B=220). Too desaturated → S < 0.18. */
    private static final int BACK_GREY   = argb(220, 220, 220);

    private GestureRecognitionService svc;

    @BeforeEach
    void setUp() {
        svc = new GestureRecognitionService();
    }

    // =========================================================================
    // 1.  isSkin() — HSV skin segmentation
    // =========================================================================

    @Test
    @DisplayName("isSkin: warm-tan skin pixel → true")
    void isSkin_warmTan_returnsTrue() {
        assertTrue(GestureRecognitionService.isSkin(SKIN_TAN),
            "Warm-tan pixel should be detected as skin");
    }

    @Test
    @DisplayName("isSkin: peach skin pixel → true")
    void isSkin_peachSkin_returnsTrue() {
        assertTrue(GestureRecognitionService.isSkin(SKIN_PEACH),
            "Peach-toned pixel should be detected as skin");
    }

    @Test
    @DisplayName("isSkin: brown skin pixel → true")
    void isSkin_brownSkin_returnsTrue() {
        assertTrue(GestureRecognitionService.isSkin(SKIN_BROWN),
            "Brown skin pixel should be detected as skin");
    }

    @Test
    @DisplayName("isSkin: saturated blue background → false")
    void isSkin_blue_returnsFalse() {
        assertFalse(GestureRecognitionService.isSkin(BACK_BLUE),
            "Blue pixel must not be classified as skin");
    }

    @Test
    @DisplayName("isSkin: vivid green → false")
    void isSkin_green_returnsFalse() {
        assertFalse(GestureRecognitionService.isSkin(BACK_GREEN),
            "Green pixel must not be classified as skin");
    }

    @Test
    @DisplayName("isSkin: near-black (too dark) → false")
    void isSkin_black_returnsFalse() {
        assertFalse(GestureRecognitionService.isSkin(BACK_BLACK),
            "Very dark pixel should not be classified as skin (brightness too low)");
    }

    @Test
    @DisplayName("isSkin: grey (desaturated) → false")
    void isSkin_grey_returnsFalse() {
        assertFalse(GestureRecognitionService.isSkin(BACK_GREY),
            "Grey pixel should not be classified as skin (saturation too low)");
    }

    // =========================================================================
    // 2.  smooth() — histogram smoothing
    // =========================================================================

    @Test
    @DisplayName("smooth: all-zero histogram stays zero")
    void smooth_allZero_remainsZero() {
        int[] hist = new int[20];
        int[] result = GestureRecognitionService.smooth(hist, 20);
        for (int v : result) assertEquals(0, v, "Smoothing all-zero should stay zero");
    }

    @Test
    @DisplayName("smooth: flat histogram stays flat (same value everywhere)")
    void smooth_flat_preservesValue() {
        int[] hist = new int[20];
        for (int i = 0; i < 20; i++) hist[i] = 50;
        int[] result = GestureRecognitionService.smooth(hist, 20);
        for (int v : result) assertEquals(50, v, "Flat histogram should remain flat after smoothing");
    }

    @Test
    @DisplayName("smooth: single impulse spreads to neighbouring bins")
    void smooth_impulse_spreadsToNeighbours() {
        int[] hist = new int[20];
        hist[10] = 100;
        int[] result = GestureRecognitionService.smooth(hist, 20);
        assertTrue(result[10] > 0, "Centre of impulse must remain > 0");
        assertTrue(result[9]  > 0, "One bin left of impulse must have non-zero value after smoothing");
        assertTrue(result[11] > 0, "One bin right of impulse must have non-zero value after smoothing");
        assertEquals(0, result[0], "Far-left bin should not be affected by impulse at index 10");
    }

    @Test
    @DisplayName("smooth: output length matches input length")
    void smooth_outputLengthMatchesInput() {
        int[] hist = {1, 2, 3, 4, 5, 6, 7};
        int[] result = GestureRecognitionService.smooth(hist, hist.length);
        assertEquals(hist.length, result.length);
    }

    // =========================================================================
    // 3.  countPeaks() — finger peak counting
    // =========================================================================

    @Test
    @DisplayName("countPeaks: all-zero histogram → 0 peaks")
    void countPeaks_allZero_returnsZero() {
        int[] hist = new int[30];
        assertEquals(0, GestureRecognitionService.countPeaks(hist, 30, 1, 3));
    }

    @Test
    @DisplayName("countPeaks: one isolated peak → 1 peak")
    void countPeaks_singlePeak_returnsOne() {
        int[] hist = new int[30];
        hist[10] = hist[11] = hist[12] = 20;  // one peak block
        assertEquals(1, GestureRecognitionService.countPeaks(hist, 30, 5, 3));
    }

    @Test
    @DisplayName("countPeaks: two well-separated peaks → 2 peaks")
    void countPeaks_twoSeparatedPeaks_returnsTwo() {
        int[] hist = new int[40];
        // Peak 1 at index 5
        hist[5] = hist[6] = 20;
        // Peak 2 at index 25 (gap = 19, well above minSep=5)
        hist[25] = hist[26] = 20;
        assertEquals(2, GestureRecognitionService.countPeaks(hist, 40, 5, 5));
    }

    @Test
    @DisplayName("countPeaks: five well-separated peaks → 5 peaks (open palm scenario)")
    void countPeaks_fiveFingerPeaks_returnsFive() {
        int[] hist = new int[80];
        int[] fingerStarts = {3, 18, 33, 48, 63};
        for (int s : fingerStarts) {
            hist[s] = hist[s + 1] = hist[s + 2] = 20;
        }
        assertEquals(5, GestureRecognitionService.countPeaks(hist, 80, 3, 5));
    }

    @Test
    @DisplayName("countPeaks: two peaks too close (< minSep) → 1 peak")
    void countPeaks_peaksTooClose_countedAsOne() {
        int[] hist = new int[20];
        hist[5] = 20;
        hist[7] = 20;  // gap = 2 < minSep=5
        // Both are above threshold but the second is within minSep of the first
        // → countPeaks: second rising edge fires while lastPeakIdx=5, i=7, i-lastPeakIdx=2 < minSep → NOT counted
        // Note: the algo sets inPeak=false at valley, so the re-entry check applies.
        // If there IS a valley between 5 and 7 (hist[6]=0), then:
        //   i=5: peak1 (peaks=1, lastPeakIdx=5, inPeak=true)
        //   i=6: hist=0 <= threshold → inPeak=false
        //   i=7: hist=20 > threshold, !inPeak, but 7-5=2 < minSep=5 → NOT counted
        assertEquals(1, GestureRecognitionService.countPeaks(hist, 20, 5, 5));
    }

    @Test
    @DisplayName("countPeaks: flat array above threshold → 1 continuous peak")
    void countPeaks_flatAboveThreshold_returnsOne() {
        int[] hist = new int[20];
        for (int i = 0; i < 20; i++) hist[i] = 10;
        assertEquals(1, GestureRecognitionService.countPeaks(hist, 20, 3, 4));
    }

    // =========================================================================
    // 4.  classify() — end-to-end gesture recognition with synthetic images
    // =========================================================================

    @Test
    @DisplayName("classify: blank image (no skin pixels) → NONE")
    void classify_blankImage_returnsNone() {
        BufferedImage img = blank(160, 90, BACK_BLUE);
        GestureRecognitionService.Gesture g = svc.classify(img, 160, 90);
        assertEquals(GestureRecognitionService.Gesture.NONE, g,
            "Image with no skin pixels should produce NONE");
    }

    @Test
    @DisplayName("classify: sparse skin pixels (< 80) → NONE")
    void classify_tooFewSkinPixels_returnsNone() {
        BufferedImage img = blank(160, 90, BACK_BLUE);
        // Paint only 40 skin pixels — below the 80-pixel threshold
        for (int x = 0; x < 40; x++) img.setRGB(x + 10, 45, SKIN_TAN);
        GestureRecognitionService.Gesture g = svc.classify(img, 160, 90);
        assertEquals(GestureRecognitionService.Gesture.NONE, g,
            "Fewer than 80 skin pixels should produce NONE");
    }

    @Test
    @DisplayName("classify: thumb column above wider palm → THUMBS_UP")
    void classify_thumbAbovePalm_returnsThumbsUp() {
        /*
         * Layout (160×90 pixels, 1:1 maps to the 160×90 processing grid):
         *
         *  rows  5-49  cols 35-40  ← narrow thumb column (6 × 45 = 270 skin)
         *  rows 50-79  cols 20-60  ← wide palm block   (41 × 30 = 1230 skin)
         *
         *  skinCount = 1500
         *  bbox: minX=20 maxX=60 minY=5 maxY=79  bboxW=41 bboxH=75
         *  aspectRatio = 75/41 ≈ 1.83  >  1.2  ✓
         *  fillRatio   = 1500/3075 ≈ 0.49  <  0.75  ✓
         *  topBoundary = 5 + (int)(75 * 0.4) = 35
         *  In rows [5..34] only thumb cols (relative idx 15-20) are skin → 1 peak ✓
         */
        BufferedImage img = blank(160, 90, BACK_BLUE);
        paintRect(img, 35, 40, 5, 49, SKIN_TAN);   // thumb
        paintRect(img, 20, 60, 50, 79, SKIN_TAN);  // palm
        GestureRecognitionService.Gesture g = svc.classify(img, 160, 90);
        assertEquals(GestureRecognitionService.Gesture.THUMBS_UP, g,
            "Narrow thumb above wide palm should classify as THUMBS_UP");
    }

    @Test
    @DisplayName("classify: five separated finger columns above palm → OPEN_PALM")
    void classify_fiveFingers_returnsOpenPalm() {
        /*
         * Layout (160×90):
         *  Fingers (each 4-wide × 30-tall), rows 5-34:
         *    cols  5- 8,  20-23,  35-38,  50-53,  65-68
         *  Palm: cols 5-68, rows 34-65
         *
         *  topBoundary = row 29 → finger peaks visible in histogram → 5 peaks ✓
         */
        BufferedImage img = blank(160, 90, BACK_BLUE);
        int[][] fingers = {{5,8},{20,23},{35,38},{50,53},{65,68}};
        for (int[] f : fingers) paintRect(img, f[0], f[1], 5, 34, SKIN_TAN);
        paintRect(img, 5, 68, 34, 65, SKIN_TAN);  // palm
        GestureRecognitionService.Gesture g = svc.classify(img, 160, 90);
        assertEquals(GestureRecognitionService.Gesture.OPEN_PALM, g,
            "Five separated finger peaks should classify as OPEN_PALM");
    }

    @Test
    @DisplayName("classify: compact filled square blob → FIST")
    void classify_compactBlob_returnsFist() {
        /*
         * Layout (160×90):
         *  Solid block: cols 20-70, rows 20-60  (51×41 = 2091 skin pixels)
         *
         *  bboxW=51 bboxH=41  aspectRatio=0.80 (<0.9)  fillRatio=1.0 (>0.55)
         *  Uniform histogram → 1 peak → fingerCount=1
         *  aspect<0.9 skips POINTING; fillRatio>0.55 → FIST ✓
         */
        BufferedImage img = blank(160, 90, BACK_BLUE);
        paintRect(img, 20, 70, 20, 60, SKIN_TAN);
        GestureRecognitionService.Gesture g = svc.classify(img, 160, 90);
        assertEquals(GestureRecognitionService.Gesture.FIST, g,
            "Compact solid blob should classify as FIST");
    }

    @Test
    @DisplayName("classify: tall slightly-rectangular solid blob → POINTING")
    void classify_tallRectBlob_returnsPointing() {
        /*
         * Layout (160×90):
         *  Solid block: cols 10-60, rows 5-56  (51×52 = 2652 skin pixels)
         *
         *  bboxW=51 bboxH=52  aspectRatio=52/51≈1.02  fillRatio=1.0
         *  fingerCount=1 (uniform histogram)
         *  aspectRatio 1.02 NOT > 1.2 → not THUMBS_UP
         *  aspectRatio 1.02 >= 0.9   → POINTING ✓
         */
        BufferedImage img = blank(160, 90, BACK_BLUE);
        paintRect(img, 10, 60, 5, 56, SKIN_TAN);
        GestureRecognitionService.Gesture g = svc.classify(img, 160, 90);
        assertEquals(GestureRecognitionService.Gesture.POINTING, g,
            "Slightly-tall rectangular blob (aspect ≈1.02) should classify as POINTING");
    }

    @Test
    @DisplayName("classify: works with larger source images (downsampling path)")
    void classify_largeSourceImage_downsampledCorrectly() {
        /*
         * A 640×480 source is downsampled to 160×90 internally.
         * We paint the THUMBS_UP shape scaled 4x to match the
         * expected downsampled layout.
         */
        BufferedImage img = blank(640, 480, BACK_BLUE);
        // Thumb: cols 140-160, rows 20-196 (≈ cols 35-40 rows 5-49 in PW×PH space)
        paintRect(img, 140, 160, 20, 196, SKIN_TAN);
        // Palm: cols 80-240, rows 200-316 (≈ cols 20-60 rows 50-79)
        paintRect(img, 80, 240, 200, 316, SKIN_TAN);
        GestureRecognitionService.Gesture g = svc.classify(img, 640, 480);
        assertEquals(GestureRecognitionService.Gesture.THUMBS_UP, g,
            "THUMBS_UP should be recognized when source image is 640×480");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Pack (A=255, R, G, B) into an ARGB int as used by BufferedImage.setRGB(). */
    private static int argb(int r, int g, int b) {
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    /** Create a BufferedImage filled entirely with the given color. */
    private static BufferedImage blank(int w, int h, int color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, color);
        return img;
    }

    /**
     * Fill a rectangle [x1..x2] × [y1..y2] (inclusive) with the given color.
     * Coordinates are clamped to image bounds.
     */
    private static void paintRect(BufferedImage img, int x1, int x2,
                                   int y1, int y2, int color) {
        int maxX = Math.min(x2, img.getWidth()  - 1);
        int maxY = Math.min(y2, img.getHeight() - 1);
        for (int y = Math.max(0, y1); y <= maxY; y++)
            for (int x = Math.max(0, x1); x <= maxX; x++)
                img.setRGB(x, y, color);
    }
}
