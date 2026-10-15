package com.shwet.job_tracker_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Any failure of the AI subsystem (LLM, embeddings, vector store).
 * The message is always a fixed, client-safe string - provider error bodies
 * are never copied into it because they can contain credential fragments.
 */
public class AiProviderException extends RuntimeException {

    public enum Kind {
        NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "The AI provider is not configured on the server."),
        UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The AI provider is currently unavailable. Please try again later."),
        RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "The AI provider rate limit was reached. Please try again later."),
        TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "The AI provider did not respond in time. Please try again."),
        BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "The AI provider returned a response that could not be used."),
        RAG_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
                "Retrieval (vector store) is unavailable. Check that RAG is enabled and pgvector is installed.");

        private final HttpStatus status;
        private final String clientMessage;

        Kind(HttpStatus status, String clientMessage) {
            this.status = status;
            this.clientMessage = clientMessage;
        }

        public HttpStatus status() {
            return status;
        }

        public String clientMessage() {
            return clientMessage;
        }
    }

    private final Kind kind;

    public AiProviderException(Kind kind) {
        super(kind.clientMessage());
        this.kind = kind;
    }

    public AiProviderException(Kind kind, Throwable cause) {
        super(kind.clientMessage(), cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public HttpStatus getStatus() {
        return kind.status();
    }
}
