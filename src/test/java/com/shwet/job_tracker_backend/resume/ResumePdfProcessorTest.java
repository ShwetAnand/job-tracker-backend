package com.shwet.job_tracker_backend.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import com.shwet.job_tracker_backend.exception.InvalidFileException;
import com.shwet.job_tracker_backend.exception.ResumeParsingException;
import com.shwet.job_tracker_backend.testsupport.PdfTestUtil;

class ResumePdfProcessorTest {

    private final ResumePdfProcessor processor =
            new ResumePdfProcessor(new ResumeProperties(DataSize.ofMegabytes(5), 3, 20, 50_000));

    private static MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("file", name, type, bytes);
    }

    private void assertInvalid(MockMultipartFile f, HttpStatus expected) {
        assertThatThrownBy(() -> processor.process(f))
                .isInstanceOfSatisfying(InvalidFileException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(expected));
    }

    @Test
    void extractsTextFromValidPdf() {
        ParsedResume parsed = processor.process(
                file("resume.pdf", "application/pdf", PdfTestUtil.pdfWithText(PdfTestUtil.SAMPLE_RESUME_TEXT)));

        assertThat(parsed.fileName()).isEqualTo("resume.pdf");
        assertThat(parsed.text()).contains("Jane Doe").contains("Spring Boot");
    }

    @Test
    void rejectsEmptyFile() {
        assertInvalid(file("resume.pdf", "application/pdf", new byte[0]), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsWrongExtension() {
        assertInvalid(file("resume.docx", "application/pdf", PdfTestUtil.pdfWithText("hello world text here")),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsWrongContentType() {
        assertInvalid(file("resume.pdf", "text/plain", PdfTestUtil.pdfWithText("hello world text here")),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsNonPdfContentEvenIfNamedPdf() {
        assertInvalid(file("resume.pdf", "application/pdf", "just some plain text".getBytes(StandardCharsets.UTF_8)),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsOversizedFile() {
        ResumePdfProcessor small =
                new ResumePdfProcessor(new ResumeProperties(DataSize.ofBytes(1024), 3, 20, 50_000));
        byte[] big = new byte[2048];
        System.arraycopy("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), 0, big, 0, 8);

        assertThatThrownBy(() -> small.process(file("resume.pdf", "application/pdf", big)))
                .isInstanceOfSatisfying(InvalidFileException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void rejectsTooManyPages() {
        assertInvalid(file("resume.pdf", "application/pdf", PdfTestUtil.pdf(4, "some text on first page only")),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void imageOnlyOrBlankPdfIsReportedAsUnreadable() {
        assertThatThrownBy(() -> processor.process(file("scan.pdf", "application/pdf", PdfTestUtil.pdf(1, ""))))
                .isInstanceOf(ResumeParsingException.class)
                .hasMessageContaining("Scanned");
    }

    @Test
    void corruptedPdfIsReportedAsParsingFailure() {
        byte[] corrupted = "%PDF-1.4\n1 0 obj << /Broken >> garbage garbage".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> processor.process(file("bad.pdf", "application/pdf", corrupted)))
                .isInstanceOf(ResumeParsingException.class);
    }

    @Test
    void fileNameIsSanitised() {
        assertThat(ResumePdfProcessor.sanitizeFileName("../../etc/evil.pdf")).isEqualTo("evil.pdf");
        assertThat(ResumePdfProcessor.sanitizeFileName(null)).isEqualTo("resume.pdf");
    }

    @Test
    void normaliseRemovesNulCharactersThatPostgresRejects() {
        assertThat(ResumePdfProcessor.normalize("a\u0000b\r\n\r\n\r\n\r\nc")).isEqualTo("ab\n\nc");
    }
}
