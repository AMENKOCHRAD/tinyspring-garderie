package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.PriseTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

@Service
public class PriseTraitementPdfService {

    public byte[] genererPdf(PriseTraitement prise) {
        if (prise == null) {
            throw new RuntimeException("Prise introuvable.");
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                final float pageW = page.getMediaBox().getWidth();
                final float pageH = page.getMediaBox().getHeight();
                final float margin = 46;

                // Theme colors (KidKinder / TinySpring)
                final int primaryR = 0x17;
                final int primaryG = 0xA2;
                final int primaryB = 0xB8;
                final int secondaryR = 0x00;
                final int secondaryG = 0x39;
                final int secondaryB = 0x4F;

                final PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                final PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                // Header band
                float headerH = 84;
                setFillRgb(cs, primaryR, primaryG, primaryB);
                cs.addRect(0, pageH - headerH, pageW, headerH);
                cs.fill();

                setFillRgb(cs, 255, 255, 255);
                drawText(cs, fontBold, 20, margin, pageH - 42, "TinySpring");
                drawText(cs, fontRegular, 12, margin, pageH - 62, "Garderie intelligente - Justificatif de prise de traitement");

                // Card container
                float cardTop = pageH - headerH - 22;
                float cardX = margin;
                float cardW = pageW - (margin * 2);
                float cardH = 520;
                float cardY = cardTop - cardH;

                setFillRgb(cs, 250, 250, 252);
                cs.addRect(cardX, cardY, cardW, cardH);
                cs.fill();

                setStrokeRgb(cs, 225, 231, 238);
                cs.setLineWidth(1.0f);
                cs.addRect(cardX, cardY, cardW, cardH);
                cs.stroke();

                // Extract values
                Traitement traitement = prise.getTraitement();
                ConditionSanitaire condition = traitement != null ? traitement.getConditionSanitaire() : null;
                Enfant enfant = condition != null ? condition.getEnfant() : null;
                User donneParUser = prise.getDonnePar();

                String enfantNom = enfant != null ? safe(enfant.getPrenom()) + " " + safe(enfant.getNom()) : "-";
                String conditionNom = condition != null ? safe(condition.getNomCondition()) : "-";
                String traitementNom = traitement != null ? safe(traitement.getNomTraitement()) : "-";
                String datePrise = prise.getDatePrise() != null ? prise.getDatePrise().toString() : "-";
                String heurePrevue = prise.getHeurePrevue() != null ? prise.getHeurePrevue() : "-";
                String donneLe = prise.getDonneLe() != null ? formatDateTime(prise.getDonneLe()) : "-";
                String donnePar = donneParUser != null ? safe(donneParUser.getNom()) : "-";
                String note = prise.getNote() != null ? prise.getNote().trim() : "";

                // Title inside card
                setFillRgb(cs, secondaryR, secondaryG, secondaryB);
                drawText(cs, fontBold, 14, cardX + 18, cardTop - 22, "Enregistrement de prise");

                // Left column fields
                float leftX = cardX + 18;
                float topY = cardTop - 54;
                float line = 18;
                float labelW = 110;

                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Reference", safePdfText("TS-" + prise.getId()));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Enfant", safePdfText(enfantNom));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Condition", safePdfText(conditionNom));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Traitement", safePdfText(traitementNom));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Date", safePdfText(datePrise));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Heure prevue", safePdfText(heurePrevue));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Donne le", safePdfText(donneLe));
                topY = drawField(cs, fontBold, fontRegular, leftX, topY, labelW, line, "Donne par", safePdfText(donnePar));

                if (!note.isBlank()) {
                    topY -= 8;
                    drawText(cs, fontBold, 11, leftX, topY, "Note");
                    topY -= 14;
                    topY = writeWrapped(cs, fontRegular, 11, leftX, topY, 14, safePdfText(note), 70);
                }

                // QR Code (right column)
                float qrSize = 150;
                float qrX = cardX + cardW - 18 - qrSize;
                float qrY = cardTop - 210;

                setStrokeRgb(cs, 225, 231, 238);
                cs.setLineWidth(1.0f);
                cs.addRect(qrX - 10, qrY - 10, qrSize + 20, qrSize + 50);
                cs.stroke();

                drawText(cs, fontBold, 11, qrX - 2, qrY + qrSize + 26, "QR code (details)");
                drawText(cs, fontRegular, 9, qrX - 2, qrY + qrSize + 12, "Scannez pour plus d'informations");

                PDImageXObject qrImage = buildQrImage(doc, buildQrPayload(prise), (int) qrSize, (int) qrSize);
                cs.drawImage(qrImage, qrX, qrY, qrSize, qrSize);

                // Footer
                setFillRgb(cs, 120, 130, 146);
                String generated = "Genere le " + formatDateTime(LocalDateTime.now()) + " - TinySpring";
                drawText(cs, fontRegular, 9, margin, 34, generated);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de generer le PDF.");
        }
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    private float drawField(PDPageContentStream cs,
                            PDType1Font bold,
                            PDType1Font regular,
                            float x,
                            float y,
                            float labelWidth,
                            float lineHeight,
                            String label,
                            String value) throws IOException {
        setFillRgb(cs, 0, 57, 79);
        drawText(cs, bold, 11, x, y, (label != null ? label : "") + " :");
        setFillRgb(cs, 30, 41, 59);
        drawText(cs, regular, 11, x + labelWidth, y, value != null ? value : "-");
        return y - lineHeight;
    }

    private float writeWrapped(PDPageContentStream cs,
                               PDType1Font font,
                               float size,
                               float x,
                               float y,
                               float lineHeight,
                               String text,
                               int maxCharsPerLine) throws IOException {
        String remaining = text != null ? text.trim() : "";
        while (!remaining.isBlank()) {
            String line;
            if (remaining.length() <= maxCharsPerLine) {
                line = remaining;
                remaining = "";
            } else {
                int cut = remaining.lastIndexOf(' ', maxCharsPerLine);
                if (cut <= 0) {
                    cut = maxCharsPerLine;
                }
                line = remaining.substring(0, cut).trim();
                remaining = remaining.substring(cut).trim();
            }
            drawText(cs, font, size, x, y, line);
            y -= lineHeight;
        }
        return y;
    }

    private PDImageXObject buildQrImage(PDDocument doc, String payload, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(
                    payload != null ? payload : "",
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
            );

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            return LosslessFactory.createFromImage(doc, image);
        } catch (Exception exception) {
            throw new RuntimeException("Impossible de generer le QR code.");
        }
    }

    private String buildQrPayload(PriseTraitement prise) {
        Traitement traitement = prise.getTraitement();
        ConditionSanitaire condition = traitement != null ? traitement.getConditionSanitaire() : null;
        Enfant enfant = condition != null ? condition.getEnfant() : null;
        User donnePar = prise.getDonnePar();

        String note = prise.getNote() != null ? prise.getNote().trim() : "";
        if (note.length() > 220) {
            note = note.substring(0, 220) + "...";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TinySpring - PriseTraitement").append('\n');
        sb.append("priseId=").append(prise.getId()).append('\n');
        if (enfant != null) {
            sb.append("enfantId=").append(enfant.getId()).append('\n');
            sb.append("enfant=").append(safe(enfant.getPrenom())).append(' ').append(safe(enfant.getNom())).append('\n');
        }
        if (condition != null) {
            sb.append("conditionId=").append(condition.getId()).append('\n');
            sb.append("condition=").append(safe(condition.getNomCondition())).append('\n');
        }
        if (traitement != null) {
            sb.append("traitementId=").append(traitement.getId()).append('\n');
            sb.append("traitement=").append(safe(traitement.getNomTraitement())).append('\n');
            sb.append("statut=").append(traitement.getStatut()).append('\n');
        }
        if (prise.getDatePrise() != null) {
            sb.append("datePrise=").append(prise.getDatePrise()).append('\n');
        }
        if (prise.getHeurePrevue() != null) {
            sb.append("heurePrevue=").append(prise.getHeurePrevue()).append('\n');
        }
        if (prise.getDonneLe() != null) {
            sb.append("donneLe=").append(formatDateTime(prise.getDonneLe())).append('\n');
        }
        if (donnePar != null) {
            sb.append("donnePar=").append(safe(donnePar.getNom())).append('\n');
            sb.append("donneParEmail=").append(safe(donnePar.getEmail())).append('\n');
        }
        if (!note.isBlank()) {
            sb.append("note=").append(note).append('\n');
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private void setFillRgb(PDPageContentStream cs, int r, int g, int b) throws IOException {
        cs.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
    }

    private void setStrokeRgb(PDPageContentStream cs, int r, int g, int b) throws IOException {
        cs.setStrokingColor(r / 255f, g / 255f, b / 255f);
    }

    /**
     * PDFBox Standard14 fonts do not support full Unicode.
     * This sanitizes text to a safe ASCII representation (removes accents, replaces unsupported chars).
     */
    private String safePdfText(String value) {
        String raw = value != null ? value.trim() : "";
        if (raw.isBlank()) {
            return raw;
        }

        // Remove accents (é -> e) and other diacritics.
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        // Keep common printable ASCII. Replace others with space.
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }

        return sb.toString().replaceAll("\\s+", " ").trim();
    }
}
