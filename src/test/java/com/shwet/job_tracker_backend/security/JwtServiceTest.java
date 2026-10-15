package com.shwet.job_tracker_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    private final JwtService jwt = new JwtService(SECRET, 60_000);

    @Test
    void generatedTokenRoundTrips() {
        String token = jwt.generateToken("a@example.com");

        assertThat(jwt.extractEmail(token)).isEqualTo("a@example.com");
        assertThat(jwt.isTokenValid(token, "a@example.com")).isTrue();
        assertThat(jwt.isTokenValid(token, "b@example.com")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000);
        String token = shortLived.generateToken("a@example.com");

        assertThatThrownBy(() -> jwt.extractEmail(token)).isInstanceOf(ExpiredJwtException.class);
        assertThat(jwt.isTokenValid(token, "a@example.com")).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("another-secret-another-secret-0123456789xx", 60_000);
        String forged = other.generateToken("a@example.com");

        assertThatThrownBy(() -> jwt.extractEmail(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedAndMalformedTokensAreRejected() {
        String token = jwt.generateToken("a@example.com");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwt.isTokenValid(tampered, "a@example.com")).isFalse();
        assertThat(jwt.isTokenValid("not-a-jwt", "a@example.com")).isFalse();
        assertThat(jwt.isTokenValid("", "a@example.com")).isFalse();
    }

    @Test
    void weakOrMissingSecretPreventsStartup() {
        assertThatThrownBy(() -> new JwtService("short", 1000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(null, 1000)).isInstanceOf(IllegalStateException.class);
    }
}
