package com.shwet.job_tracker_backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.shwet.job_tracker_backend.testsupport.PdfTestUtil;

class UserApiTest extends AbstractApiTest {

    private Map<String, String> update(String name, String email) {
        return Map.of("name", name, "email", email, "password", PASSWORD);
    }

    @Test
    void meReturnsOwnProfileWithoutPassword() throws Exception {
        TestUser a = newUser();

        mvc.perform(as(a, get("/api/users/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.id()))
                .andExpect(jsonPath("$.email").value(a.email()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void userCannotReadUpdateOrDeleteAnotherAccount() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();

        mvc.perform(as(a, get("/api/users/" + b.id()))).andExpect(status().isNotFound());

        mvc.perform(as(a, put("/api/users/" + b.id())).contentType(MediaType.APPLICATION_JSON)
                        .content(json(update("Hacked", "hacked@example.com"))))
                .andExpect(status().isNotFound());

        mvc.perform(as(a, delete("/api/users/" + b.id()))).andExpect(status().isNotFound());

        // B is untouched and can still log in
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", b.email(), "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void ownAccountCanBeUpdatedThroughBothRoutes() throws Exception {
        TestUser a = newUser();

        mvc.perform(as(a, put("/api/users/me")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(update("New Name", a.email()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));

        mvc.perform(as(a, put("/api/users/" + a.id())).contentType(MediaType.APPLICATION_JSON)
                        .content(json(update("Newer Name", a.email()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Newer Name"));
    }

    @Test
    void cannotTakeAnotherUsersEmailOnUpdate() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();

        mvc.perform(as(a, put("/api/users/me")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(update("A", b.email()))))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingAccountRemovesItsJobsAndResumesWithoutConstraintErrors() throws Exception {
        TestUser a = newUser();
        createJob(a, "Acme", "Dev", "APPLIED", "2026-01-01", null);
        mvc.perform(as(a, multipart("/api/resumes/upload").file(new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", PdfTestUtil.pdfWithText(PdfTestUtil.SAMPLE_RESUME_TEXT)))))
                .andExpect(status().isCreated());

        mvc.perform(as(a, delete("/api/users/me"))).andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", a.email(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }
}
