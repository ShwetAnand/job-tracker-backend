package com.shwet.job_tracker_backend.exception;

import org.springframework.http.HttpStatus;

/** An uploaded file is rejected (wrong type, empty, too large, ...). */
public class InvalidFileException extends RuntimeException {

    private final HttpStatus status;

    public InvalidFileException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
