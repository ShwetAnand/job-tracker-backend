package com.shwet.job_tracker_backend.exception;

/** The request is syntactically valid but violates a business/validation rule (HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
