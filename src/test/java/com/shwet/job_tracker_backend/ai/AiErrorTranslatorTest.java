package com.shwet.job_tracker_backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.shwet.job_tracker_backend.exception.AiProviderException;
import com.shwet.job_tracker_backend.exception.AiProviderException.Kind;

class AiErrorTranslatorTest {

    private static RestClientResponseException http(int status) {
        return new RestClientResponseException("Incorrect API key provided: sk-SECRET", status, "x", null, null, null);
    }

    @Test
    void http429IsRateLimited() {
        assertThat(AiErrorTranslator.translate(new RuntimeException("wrapped", http(429))).getKind())
                .isEqualTo(Kind.RATE_LIMITED);
    }

    @Test
    void http401IsNotConfigured() {
        assertThat(AiErrorTranslator.translate(http(401)).getKind()).isEqualTo(Kind.NOT_CONFIGURED);
    }

    @Test
    void http503IsUnavailable() {
        assertThat(AiErrorTranslator.translate(http(503)).getKind()).isEqualTo(Kind.UNAVAILABLE);
    }

    @Test
    void socketTimeoutIsTimeout() {
        RuntimeException e = new ResourceAccessException("io", new SocketTimeoutException("Read timed out"));
        assertThat(AiErrorTranslator.translate(e).getKind()).isEqualTo(Kind.TIMEOUT);
    }

    @Test
    void messageNeverLeaksProviderDetails() {
        AiProviderException translated = AiErrorTranslator.translate(http(401));
        assertThat(translated.getMessage()).doesNotContain("sk-SECRET");
        assertThat(translated.getStatus().value()).isEqualTo(503);
    }

    @Test
    void existingAiProviderExceptionIsPassedThrough() {
        AiProviderException original = new AiProviderException(Kind.BAD_RESPONSE);
        assertThat(AiErrorTranslator.translate(original)).isSameAs(original);
    }
}
