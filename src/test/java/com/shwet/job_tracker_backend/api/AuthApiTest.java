package com.shwet.job_tracker_backend.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import com.shwet.job_tracker_backend.security.JwtService;

class AuthApiTest extends AbstractApiTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void registrationReturnsUserWithoutPasswordAndNormalisesEmail() throws Exception {
        String email = uniqueEmail();

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Jane", "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void duplicateRegistrationIsConflictEvenWithDifferentCase() throws Exception {
        TestUser existing = newUser();

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Dup", "email", existing.email().toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void registrationValidationErrorsUseTheStandardErrorShape() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void malformedJsonBodyIsBadRequestWithoutInternals() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void loginSucceedsAndReturnsToken() throws Exception {
        TestUser user = newUser();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user.id()));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSame401() throws Exception {
        TestUser user = newUser();

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", "wrong-password-1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", "wrong-password-1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointWithoutTokenIs401NotForbidden() throws Exception {
        mvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedTokenIs401() throws Exception {
        mvc.perform(get("/api/jobs").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        TestUser user = newUser();
        String expired = new JwtService(jwtSecret, -5_000).generateToken(user.email());

        mvc.perform(get("/api/jobs").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() throws Exception {
        TestUser user = newUser();
        String forged = new JwtService("a-completely-different-secret-0123456789", 60_000).generateToken(user.email());

        mvc.perform(get("/api/jobs").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        TestUser user = newUser();

        mvc.perform(as(user, get("/api/jobs"))).andExpect(status().isOk());
    }

    @Test
    void tokenOfADeletedUserIsRejected() throws Exception {
        TestUser user = newUser();

        mvc.perform(as(user, delete("/api/users/me"))).andExpect(status().isOk());

        mvc.perform(as(user, get("/api/jobs"))).andExpect(status().isUnauthorized());
    }

    @Test
    void badTokenOnPublicEndpointDoesNotBreakIt() throws Exception {
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", "whatever-123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownPathIs404AndWrongMethodIs405WithJsonBodies() throws Exception {
        TestUser user = newUser();

        mvc.perform(as(user, get("/api/does-not-exist")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mvc.perform(as(user, get("/api/users")))   // list-all-users was removed
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", not(containsString("Exception"))));
    }
}
