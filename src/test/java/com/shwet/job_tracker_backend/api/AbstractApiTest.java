package com.shwet.job_tracker_backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Full Spring context + real security filter chain + in-memory H2 (profile "test"). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiTest {

    protected static final String PASSWORD = "Passw0rd!long";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    protected JsonNode body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    /** Registers a user and returns {id, email, token}. */
    protected TestUser newUser() throws Exception {
        String email = uniqueEmail();

        JsonNode created = body(mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Test User", "email", email, "password", PASSWORD)))));

        JsonNode login = body(mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", PASSWORD)))));

        return new TestUser(created.get("id").asLong(), email, login.get("token").asText());
    }

    protected MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer(user.token()));
    }

    protected long createJob(TestUser user, String company, String role, String status, String date, String description)
            throws Exception {

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("companyName", company);
        payload.put("role", role);
        payload.put("status", status);
        payload.put("appliedDate", date);
        if (description != null) {
            payload.put("jobDescription", description);
        }

        return body(mvc.perform(as(user, post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(payload))))).get("id").asLong();
    }

    protected record TestUser(long id, String email, String token) {
    }
}
