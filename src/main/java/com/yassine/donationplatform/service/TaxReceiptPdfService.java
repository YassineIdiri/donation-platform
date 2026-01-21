package com.yassine.donationplatform.service;

import com.yassine.donationplatform.domain.donation.Donation;
import com.yassine.donationplatform.domain.receipt.TaxReceipt;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaxReceiptPdfService {

    @Value("${app.receipt.storage-dir:./storage/receipts}")
    private String storageDir;

    @Value("${app.receipt.org-name:Association}")
    private String orgName;

    @Value("${app.receipt.org-address:}")
    private String orgAddress;

    @Value("${app.receipt.org-identifier:}")
    private String orgIdentifier;

    @Value("${app.receipt.org-email:}")
    private String orgEmail;

    public Path generatePdf(TaxReceipt receipt, Donation donation) {
        try {
            Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // ✅ référence affichée + utilisée dans le nom de fichier
            String receiptRef = formatReceiptRef(receipt); // CERFA-000123 ou DRAFT-<uuid>

            String filename = "recu-fiscal-" + receiptRef + ".pdf";
            Path out = dir.resolve(filename);

            // ✅ si déjà généré et non vide : on réutilise
            if (Files.exists(out) && Files.size(out) > 0) {
                return out;
            }

            // ✅ écrire dans tmp puis move atomique (évite corruption / overwrite)
            Path tmp = dir.resolve(filename + ".tmp");

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                float margin = 50f;
                float yStart = 780f;
                float leading = 16f;
                float maxWidth = page.getMediaBox().getWidth() - 2 * margin;

                PDType1Font fontTitle = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontBody  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                    // --- Titre
                    cs.beginText();
                    cs.setFont(fontTitle, 16);
                    cs.newLineAtOffset(margin, yStart);
                    cs.showText("Reçu fiscal (don)");
                    cs.endText();

                    // --- Corps
                    cs.beginText();
                    cs.setFont(fontBody, 11);
                    cs.setLeading(leading);
                    cs.newLineAtOffset(margin, yStart - 2 * leading);

                    line(cs, "N° reçu : " + receiptRef);
                    line(cs, "Date du don : " + formatInstant(donation.getCreatedAt()));
                    line(cs, "Montant : " + formatAmount(donation) + " " + nullSafe(donation.getCurrency()));

                    blank(cs);

                    wrapLines(cs, fontBody, 11, maxWidth, "Donateur : " + nullSafe(receipt.getDonorFullName()));
                    wrapLines(cs, fontBody, 11, maxWidth, "Email : " + nullSafe(receipt.getEmail()));
                    wrapLines(cs, fontBody, 11, maxWidth, "Adresse : " + nullSafe(receipt.getDonorAddress()));

                    blank(cs);

                    wrapLines(cs, fontBody, 11, maxWidth, "Organisme bénéficiaire : " + nullSafe(orgName));
                    if (!isBlank(orgAddress))    wrapLines(cs, fontBody, 11, maxWidth, "Adresse : " + orgAddress);
                    if (!isBlank(orgIdentifier)) wrapLines(cs, fontBody, 11, maxWidth, "Identifiant : " + orgIdentifier);
                    if (!isBlank(orgEmail))      wrapLines(cs, fontBody, 11, maxWidth, "Contact : " + orgEmail);

                    blank(cs);

                    wrapLines(cs, fontBody, 11, maxWidth, "Ce document est émis à titre de justificatif de don.");
                    wrapLines(cs, fontBody, 11, maxWidth,
                            "L'association est responsable de l'éligibilité fiscale et des mentions obligatoires.");

                    cs.endText();
                }

                doc.save(tmp.toFile());
            }

            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return out;

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    // ---------------- helpers ----------------

    private static String formatReceiptRef(TaxReceipt receipt) {
        Long n = receipt.getReceiptNumber();
        if (n != null) {
            return String.format("CERFA-%06d", n);
        }
        if (receipt.getId() != null) {
            return "DRAFT-" + receipt.getId();
        }
        return "DRAFT";
    }

    private static void line(PDPageContentStream cs, String text) throws IOException {
        cs.showText(text != null ? text : "");
        cs.newLine();
    }

    private static void blank(PDPageContentStream cs) throws IOException {
        cs.newLine();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of("Europe/Paris"))
                .format(instant);
    }

    private static String formatAmount(Donation d) {
        if (d == null) return "";
        BigDecimal euros = BigDecimal.valueOf(d.getAmountCents())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return euros.toPlainString();
    }

    private static void wrapLines(PDPageContentStream cs,
                                  PDType1Font font,
                                  int fontSize,
                                  float maxWidth,
                                  String text) throws IOException {
        for (String l : wrap(font, fontSize, maxWidth, text)) {
            line(cs, l);
        }
    }

    private static List<String> wrap(PDType1Font font, int fontSize, float maxWidth, String text) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String w : words) {
            String candidate = current.isEmpty() ? w : current + " " + w;
            float width = (font.getStringWidth(candidate) / 1000f) * fontSize;

            if (width <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current.setLength(0);
                current.append(w);
            }
        }

        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }
}
