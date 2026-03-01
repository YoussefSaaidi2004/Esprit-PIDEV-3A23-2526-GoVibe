package org.example.utils;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.example.entities.Forum;
import org.example.entities.Poste;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * PDFBox-based exporter for GoVibe forum and post reports.
 */
public class ForumPdfExporter {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 22f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    // Emerald brand color
    private static final PDColor EMERALD = new PDColor(
            new float[]{0.31f, 0.78f, 0.47f}, PDDeviceRGB.INSTANCE);
    private static final PDColor DARK_GREEN = new PDColor(
            new float[]{0.004f, 0.18f, 0.125f}, PDDeviceRGB.INSTANCE);
    private static final PDColor LIGHT_GRAY = new PDColor(
            new float[]{0.95f, 0.96f, 0.97f}, PDDeviceRGB.INSTANCE);
    private static final PDColor TEXT_DARK = new PDColor(
            new float[]{0.1f, 0.12f, 0.11f}, PDDeviceRGB.INSTANCE);

    /**
     * Exports a list of forums to a PDF file.
     */
    public static void exportForums(List<Forum> forums, File outputFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            PDFont regularFont = PDType1Font.HELVETICA;
            PDFont oblique = PDType1Font.HELVETICA_OBLIQUE;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header banner
                cs.setNonStrokingColor(DARK_GREEN);
                cs.addRect(0, PAGE_HEIGHT - 90, PAGE_WIDTH, 90);
                cs.fill();

                // GoVibe title
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(boldFont, 26);
                cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - 45);
                cs.showText("GoVibe — Rapport Forums");
                cs.endText();

                // Date
                String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                cs.setNonStrokingColor(0.7f, 0.9f, 0.75f);
                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - 65);
                cs.showText("Exporté le " + date + "   |   " + forums.size() + " forum(s) au total");
                cs.endText();

                y = PAGE_HEIGHT - 115;

                // Stats row
                int totalMembers = forums.stream().mapToInt(Forum::getNbr_members).sum();
                int totalPosts = forums.stream().mapToInt(Forum::getPost_count).sum();
                drawStatBox(cs, boldFont, regularFont, MARGIN, y, CONTENT_WIDTH / 3 - 10,
                        "FORUMS", String.valueOf(forums.size()));
                drawStatBox(cs, boldFont, regularFont, MARGIN + CONTENT_WIDTH / 3 + 5, y, CONTENT_WIDTH / 3 - 10,
                        "MEMBRES", String.valueOf(totalMembers));
                drawStatBox(cs, boldFont, regularFont, MARGIN + 2 * CONTENT_WIDTH / 3 + 10, y, CONTENT_WIDTH / 3 - 10,
                        "PUBLICATIONS", String.valueOf(totalPosts));

                y -= 80;

                // Section title
                cs.setNonStrokingColor(DARK_GREEN);
                cs.beginText();
                cs.setFont(boldFont, 13);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Liste des Forums");
                cs.endText();
                y -= 8;

                // Separator line
                cs.setStrokingColor(EMERALD);
                cs.setLineWidth(2f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 18;

                // Table header
                y = drawTableHeader(cs, boldFont, y);

                // Table rows
                boolean alternate = false;
                for (Forum f : forums) {
                    if (y < 80) {
                        // new page
                        cs.close();
                        PDPage nextPage = new PDPage(PDRectangle.A4);
                        doc.addPage(nextPage);
                        // Note: can't reuse cs here easily with try-with-resources
                        // For simplicity, just break if out of space
                        break;
                    }
                    y = drawForumRow(cs, regularFont, oblique, f, y, alternate);
                    alternate = !alternate;
                }

                // Footer
                drawFooter(cs, regularFont, date);
            }

            doc.save(outputFile);
        }
    }

    /**
     * Exports posts sorted by likes descending.
     */
    public static void exportPosts(List<Poste> posts, File outputFile) throws IOException {
        List<Poste> sorted = posts.stream()
                .sorted(Comparator.comparingInt(Poste::getLikes).reversed())
                .toList();

        try (PDDocument doc = new PDDocument()) {
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            PDFont regularFont = PDType1Font.HELVETICA;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header
                cs.setNonStrokingColor(DARK_GREEN);
                cs.addRect(0, PAGE_HEIGHT - 90, PAGE_WIDTH, 90);
                cs.fill();

                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(boldFont, 26);
                cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - 45);
                cs.showText("GoVibe — Rapport Publications");
                cs.endText();

                String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                cs.setNonStrokingColor(0.7f, 0.9f, 0.75f);
                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - 65);
                cs.showText("Exporté le " + date + "   |   " + sorted.size() + " publication(s)   |   Triées par likes");
                cs.endText();

                y = PAGE_HEIGHT - 115;

                // Stats
                int totalLikes = sorted.stream().mapToInt(Poste::getLikes).sum();
                int maxLikes = sorted.isEmpty() ? 0 : sorted.get(0).getLikes();
                drawStatBox(cs, boldFont, regularFont, MARGIN, y, CONTENT_WIDTH / 3 - 10,
                        "PUBLICATIONS", String.valueOf(sorted.size()));
                drawStatBox(cs, boldFont, regularFont, MARGIN + CONTENT_WIDTH / 3 + 5, y, CONTENT_WIDTH / 3 - 10,
                        "TOTAL LIKES", String.valueOf(totalLikes));
                drawStatBox(cs, boldFont, regularFont, MARGIN + 2 * CONTENT_WIDTH / 3 + 10, y, CONTENT_WIDTH / 3 - 10,
                        "MAX LIKES", String.valueOf(maxLikes));

                y -= 80;

                // Section title
                cs.setNonStrokingColor(DARK_GREEN);
                cs.beginText();
                cs.setFont(boldFont, 13);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Publications Classées par Likes ❤️");
                cs.endText();
                y -= 8;

                cs.setStrokingColor(EMERALD);
                cs.setLineWidth(2f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 18;

                // Post table header
                cs.setNonStrokingColor(DARK_GREEN);
                cs.addRect(MARGIN, y - 16, CONTENT_WIDTH, 24);
                cs.fill();

                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(boldFont, 9);
                cs.newLineAtOffset(MARGIN + 5, y - 8);
                cs.showText("RANG");
                cs.newLineAtOffset(45, 0);
                cs.showText("LIKES");
                cs.newLineAtOffset(50, 0);
                cs.showText("TYPE");
                cs.newLineAtOffset(60, 0);
                cs.showText("CONTENU");
                cs.newLineAtOffset(230, 0);
                cs.showText("DATE");
                cs.endText();
                y -= 28;

                boolean alt = false;
                int rank = 1;
                for (Poste p : sorted) {
                    if (y < 80) break;

                    if (alt) {
                        cs.setNonStrokingColor(0.96f, 0.98f, 0.97f);
                        cs.addRect(MARGIN, y - 14, CONTENT_WIDTH, 22);
                        cs.fill();
                    }

                    String contenu = p.getContenu() != null
                            ? p.getContenu().replaceAll("\\s+", " ").substring(0, Math.min(p.getContenu().length(), 55)) + "…"
                            : "(vide)";
                    String dateStr = p.getDate_creation() != null
                            ? p.getDate_creation().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                            : "-";

                    cs.setNonStrokingColor(TEXT_DARK);
                    cs.beginText();
                    cs.setFont(rank <= 3 ? boldFont : regularFont, 9);
                    cs.newLineAtOffset(MARGIN + 5, y - 8);
                    cs.showText("#" + rank);
                    cs.newLineAtOffset(45, 0);
                    cs.setNonStrokingColor(0.31f, 0.78f, 0.47f);
                    cs.showText(String.valueOf(p.getLikes()));
                    cs.setNonStrokingColor(TEXT_DARK);
                    cs.newLineAtOffset(50, 0);
                    cs.showText(p.getType() != null ? p.getType() : "-");
                    cs.newLineAtOffset(60, 0);
                    cs.showText(contenu);
                    cs.newLineAtOffset(230, 0);
                    cs.showText(dateStr);
                    cs.endText();

                    y -= 22;
                    rank++;
                    alt = !alt;
                }

                drawFooter(cs, regularFont, date);
            }

            doc.save(outputFile);
        }
    }

    private static float drawTableHeader(PDPageContentStream cs, PDFont boldFont, float y) throws IOException {
        cs.setNonStrokingColor(DARK_GREEN);
        cs.addRect(MARGIN, y - 16, CONTENT_WIDTH, 24);
        cs.fill();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(boldFont, 9);
        cs.newLineAtOffset(MARGIN + 5, y - 8);
        cs.showText("NOM DU FORUM");
        cs.newLineAtOffset(155, 0);
        cs.showText("MEMBRES");
        cs.newLineAtOffset(70, 0);
        cs.showText("PUBLICATIONS");
        cs.newLineAtOffset(95, 0);
        cs.showText("DESCRIPTION");
        cs.newLineAtOffset(110, 0);
        cs.showText("CRÉÉ LE");
        cs.endText();
        return y - 28;
    }

    private static float drawForumRow(PDPageContentStream cs, PDFont regular, PDFont oblique,
                                       Forum f, float y, boolean alternate) throws IOException {
        if (alternate) {
            cs.setNonStrokingColor(0.96f, 0.98f, 0.97f);
            cs.addRect(MARGIN, y - 14, CONTENT_WIDTH, 22);
            cs.fill();
        }

        String desc = f.getDescription() != null
                ? f.getDescription().substring(0, Math.min(f.getDescription().length(), 35)) + "…"
                : "—";
        String dateStr = f.getDate_creation() != null
                ? f.getDate_creation().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                : "—";
        String name = f.getName() != null
                ? f.getName().substring(0, Math.min(f.getName().length(), 22))
                : "—";

        cs.setNonStrokingColor(TEXT_DARK);
        cs.beginText();
        cs.setFont(regular, 9);
        cs.newLineAtOffset(MARGIN + 5, y - 8);
        cs.showText(name);
        cs.newLineAtOffset(155, 0);
        cs.showText(String.valueOf(f.getNbr_members()));
        cs.newLineAtOffset(70, 0);
        cs.showText(String.valueOf(f.getPost_count()));
        cs.newLineAtOffset(95, 0);
        cs.setFont(oblique, 8.5f);
        cs.showText(desc);
        cs.newLineAtOffset(110, 0);
        cs.setFont(regular, 9);
        cs.showText(dateStr);
        cs.endText();

        return y - 22;
    }

    private static void drawStatBox(PDPageContentStream cs, PDFont bold, PDFont regular,
                                     float x, float y, float w, String label, String value) throws IOException {
        cs.setNonStrokingColor(LIGHT_GRAY);
        cs.addRect(x, y - 50, w, 58);
        cs.fill();

        // Emerald left accent
        cs.setNonStrokingColor(EMERALD);
        cs.addRect(x, y - 50, 4, 58);
        cs.fill();

        cs.setNonStrokingColor(0.31f, 0.78f, 0.47f);
        cs.beginText();
        cs.setFont(bold, 22);
        cs.newLineAtOffset(x + 14, y - 22);
        cs.showText(value);
        cs.endText();

        cs.setNonStrokingColor(0.4f, 0.45f, 0.42f);
        cs.beginText();
        cs.setFont(regular, 8);
        cs.newLineAtOffset(x + 14, y - 40);
        cs.showText(label);
        cs.endText();
    }

    private static void drawFooter(PDPageContentStream cs, PDFont regular, String date) throws IOException {
        cs.setNonStrokingColor(DARK_GREEN);
        cs.addRect(0, 0, PAGE_WIDTH, 35);
        cs.fill();

        cs.setNonStrokingColor(0.7f, 0.9f, 0.75f);
        cs.beginText();
        cs.setFont(regular, 8);
        cs.newLineAtOffset(MARGIN, 14);
        cs.showText("GoVibe — Rapport généré le " + date + "   |   © 2024 GoVibe. Tous droits réservés.");
        cs.endText();
    }
}
