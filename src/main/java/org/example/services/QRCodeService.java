package org.example.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * 🎟 QR Code Service — GoVibe Hotel Module
 * Generates unique QR codes for reservations (check-in verification).
 */
public class QRCodeService {

    private static final int DEFAULT_SIZE = 250;

    /**
     * Generate a JavaFX-compatible QR Code image from text content.
     */
    public static WritableImage generateQRCode(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bitMatrix.get(x, y)) {
                    // Dark modules — deep green for GoVibe brand
                    pixelWriter.setColor(x, y, Color.web("#013220"));
                } else {
                    // Light modules — white background
                    pixelWriter.setColor(x, y, Color.WHITE);
                }
            }
        }
        return image;
    }

    /**
     * Generate a QR Code at default size (250x250).
     */
    public static WritableImage generateQRCode(String content) throws WriterException {
        return generateQRCode(content, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Build the QR content string for a reservation.
     * Format: structured data block for check-in scanner.
     */
    public static String buildReservationQRContent(int reservationId, int userId,
                                                    String hotelName, String chambreType,
                                                    String dateDebut, String dateFin,
                                                    double prixTotal) {
        return String.format(
            "GOVIBE-HOTEL-RESERVATION\n" +
            "ID:%d\n" +
            "UTILISATEUR:%d\n" +
            "HOTEL:%s\n" +
            "CHAMBRE:%s\n" +
            "ARRIVEE:%s\n" +
            "DEPART:%s\n" +
            "PRIX:%.2f DT\n" +
            "STATUT:VALIDE",
            reservationId, userId, hotelName, chambreType, dateDebut, dateFin, prixTotal
        );
    }

    /**
     * Build simple QR content by reservation ID only.
     */
    public static String buildSimpleQRContent(int reservationId) {
        return "GOVIBE-RESERVATION-" + reservationId;
    }
}
