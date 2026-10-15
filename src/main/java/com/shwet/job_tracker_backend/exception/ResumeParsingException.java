package com.shwet.job_tracker_backend.exception;

/**
 * A syntactically accepted PDF could not be turned into usable text
 * (corrupt, encrypted, or image-only/scanned). Maps to HTTP 422.
 */
public class ResumeParsingException extends RuntimeException {

    public ResumeParsingException(String message) {
        super(message);
    }

    public ResumeParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
