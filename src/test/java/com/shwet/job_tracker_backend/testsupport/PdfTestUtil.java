package com.shwet.job_tracker_backend.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Builds small real PDFs for tests. */
public final class PdfTestUtil {

    private PdfTestUtil() {
    }

    public static final String SAMPLE_RESUME_TEXT =
            "Jane Doe\nBackend developer\nSkills: Java, Spring Boot, PostgreSQL, Docker, REST APIs\n"
                    + "Built and deployed several microservices with JWT authentication.";

    /** One page containing the given text (lines separated by \n). */
    public static byte[] pdfWithText(String text) {
        return pdf(1, text);
    }

    /** {@code pages} pages; the text is written on the first page only. Empty text gives blank pages. */
    public static byte[] pdf(int pages, String text) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (p == 0 && text != null && !text.isEmpty()) {
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                        cs.setLeading(14f);
                        cs.newLineAtOffset(40, 740);
                        for (String line : text.split("\n")) {
                            cs.showText(line);
                            cs.newLine();
                        }
                        cs.endText();
                    }
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
