package com.shwet.job_tracker_backend.resume;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.shwet.job_tracker_backend.exception.InvalidFileException;
import com.shwet.job_tracker_backend.exception.ResumeParsingException;

import lombok.RequiredArgsConstructor;

/**
 * Validates an uploaded resume and extracts its text with Apache PDFBox.
 *
 * Checks (cheap ones first): present/non-empty, size, extension, declared content type,
 * "%PDF-" signature, then a real parse (page limit, encryption, malformed files, and
 * image-only PDFs that yield no text).
 *
 * OCR is NOT implemented: scanned PDFs are rejected with a clear message.
 */
@Component
@RequiredArgsConstructor
public class ResumePdfProcessor {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("application/pdf", "application/x-pdf", "application/octet-stream");

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    /** The PDF spec allows the signature anywhere in the first 1024 bytes. */
    private static final int SIGNATURE_SEARCH_WINDOW = 1024;

    private final ResumeProperties properties;

    public ParsedResume process(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException(HttpStatus.BAD_REQUEST, "Resume file is required and must not be empty");
        }

        if (file.getSize() > properties.maxSize().toBytes()) {
            throw new InvalidFileException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Resume file is too large (maximum " + properties.maxSize().toMegabytes() + " MB)");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidFileException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF resume files are allowed");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF resume files are allowed");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException(HttpStatus.BAD_REQUEST, "The uploaded file could not be read");
        }

        if (!hasPdfSignature(bytes)) {
            throw new InvalidFileException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The uploaded file is not a valid PDF document");
        }

        String text = normalize(extractText(bytes));

        if (text.length() < properties.minTextChars()) {
            throw new ResumeParsingException(
                    "No readable text was found in this PDF. Scanned or image-only resumes are not supported; "
                            + "please upload a PDF with selectable text.");
        }

        if (text.length() > properties.maxTextChars()) {
            text = text.substring(0, properties.maxTextChars());
        }

        return new ParsedResume(fileName, text);
    }

    private String extractText(byte[] bytes) {

        try (PDDocument document = Loader.loadPDF(bytes)) {

            if (document.getNumberOfPages() > properties.maxPages()) {
                throw new InvalidFileException(HttpStatus.BAD_REQUEST,
                        "The PDF has too many pages (maximum " + properties.maxPages() + ")");
            }

            return new PDFTextStripper().getText(document);

        } catch (InvalidFileException e) {
            throw e;
        } catch (InvalidPasswordException e) {
            throw new ResumeParsingException("Password-protected PDFs are not supported", e);
        } catch (IOException | RuntimeException e) {
            // PDFBox can throw unchecked exceptions on damaged files as well.
            throw new ResumeParsingException("The PDF could not be read; it may be corrupted", e);
        }
    }

    private boolean hasPdfSignature(byte[] bytes) {

        int limit = Math.min(bytes.length, SIGNATURE_SEARCH_WINDOW) - PDF_SIGNATURE.length;

        for (int i = 0; i <= limit; i++) {
            boolean match = true;
            for (int j = 0; j < PDF_SIGNATURE.length; j++) {
                if (bytes[i + j] != PDF_SIGNATURE[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    /** Removes path parts and control characters from a client-supplied file name. */
    static String sanitizeFileName(String original) {

        String name = original == null ? "" : StringUtils.getFilename(StringUtils.cleanPath(original));
        name = name == null ? "" : name.replaceAll("\\p{Cntrl}", "").trim();

        if (name.isEmpty()) {
            return "resume.pdf";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    /**
     * PostgreSQL rejects NUL characters in text columns, and PDF extraction sometimes produces
     * them (or other control characters), so strip those and tidy whitespace.
     */
    static String normalize(String raw) {

        if (raw == null) {
            return "";
        }

        return raw
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
