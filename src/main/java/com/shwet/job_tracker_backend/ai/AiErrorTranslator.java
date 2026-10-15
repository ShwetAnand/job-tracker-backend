package com.shwet.job_tracker_backend.ai;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.shwet.job_tracker_backend.exception.AiProviderException;
import com.shwet.job_tracker_backend.exception.AiProviderException.Kind;

/**
 * Converts whatever a Spring AI / HTTP client call throws into an
 * {@link AiProviderException} with a client-safe message.
 *
 * The original exception is logged by class and HTTP status only: provider
 * error bodies (and therefore exception messages) may contain parts of the API key.
 */
public final class AiErrorTranslator {

    private static final Logger log = LoggerFactory.getLogger(AiErrorTranslator.class);

    private AiErrorTranslator() {
    }

    public static AiProviderException translate(RuntimeException error) {

        if (error instanceof AiProviderException already) {
            return already;
        }

        Kind kind = classify(error);

        log.warn("AI call failed ({}): {}", kind, describe(error));

        return new AiProviderException(kind);
    }

    static Kind classify(Throwable error) {

        for (Throwable t = error; t != null; t = t.getCause()) {

            if (t instanceof RestClientResponseException http) {
                int status = http.getStatusCode().value();
                if (status == 429) {
                    return Kind.RATE_LIMITED;
                }
                if (status == 401 || status == 403) {
                    return Kind.NOT_CONFIGURED;
                }
                return Kind.UNAVAILABLE;
            }

            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return Kind.TIMEOUT;
            }

            if (t instanceof ConnectException) {
                return Kind.UNAVAILABLE;
            }

            if (t instanceof ResourceAccessException) {
                // I/O problem talking to the provider; a timeout cause is detected on the next loop turn
                continue;
            }
        }

        // Spring AI sometimes wraps HTTP failures in its own exception type without a typed cause.
        String message = error.getMessage();
        if (message != null && message.contains("429")) {
            return Kind.RATE_LIMITED;
        }

        return Kind.UNAVAILABLE;
    }

    private static String describe(Throwable error) {
        StringBuilder sb = new StringBuilder(error.getClass().getName());
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof RestClientResponseException http) {
                sb.append(" [HTTP ").append(http.getStatusCode().value()).append(']');
                break;
            }
        }
        return sb.toString();
    }
}
