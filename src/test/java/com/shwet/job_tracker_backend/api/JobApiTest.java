package com.shwet.job_tracker_backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class JobApiTest extends AbstractApiTest {

    private Map<String, Object> jobPayload(String company) {
        return Map.of("companyName", company, "role", "Engineer", "status", "APPLIED", "appliedDate", "2026-01-15");
    }

    @Test
    void createAssignsTheAuthenticatedUserAndIgnoresClientSuppliedUserId() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();

        Map<String, Object> payload = new java.util.HashMap<>(jobPayload("Acme"));
        payload.put("userId", b.id());   // attempt to create a job for someone else

        mvc.perform(as(a, post("/api/jobs")).contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(a.id()));

        mvc.perform(as(b, get("/api/jobs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crudHappyPath() throws Exception {
        TestUser a = newUser();
        long id = createJob(a, "Acme", "Backend", "APPLIED", "2026-01-01", null);

        mvc.perform(as(a, get("/api/jobs/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme"));

        mvc.perform(as(a, put("/api/jobs/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("companyName", "Acme 2", "role", "Lead", "status", "INTERVIEW",
                                "appliedDate", "2026-01-02"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme 2"))
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        mvc.perform(as(a, delete("/api/jobs/" + id))).andExpect(status().isOk());
        mvc.perform(as(a, get("/api/jobs/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void userBCannotReadUpdateOrDeleteUserAsJob() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        long id = createJob(a, "Secret Corp", "Spy", "APPLIED", "2026-01-01", null);

        mvc.perform(as(b, get("/api/jobs/" + id))).andExpect(status().isNotFound());

        mvc.perform(as(b, put("/api/jobs/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(jobPayload("Hijacked"))))
                .andExpect(status().isNotFound());

        mvc.perform(as(b, delete("/api/jobs/" + id))).andExpect(status().isNotFound());

        // untouched for the owner
        mvc.perform(as(a, get("/api/jobs/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Secret Corp"));
    }

    @Test
    void otherUsersJobsNeverAppearInListSearchFilterOrPage() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        createJob(a, "Acme", "Dev", "APPLIED", "2026-01-01", null);

        mvc.perform(as(b, get("/api/jobs"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(as(b, get("/api/jobs/search").param("keyword", "acme"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(as(b, get("/api/jobs/filter").param("status", "APPLIED"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(as(b, get("/api/jobs/page"))).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidAndUnknownIds() throws Exception {
        TestUser a = newUser();

        mvc.perform(as(a, get("/api/jobs/abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

        mvc.perform(as(a, get("/api/jobs/999999999"))).andExpect(status().isNotFound());
    }

    @Test
    void validationRejectsMissingFields() throws Exception {
        TestUser a = newUser();

        mvc.perform(as(a, post("/api/jobs")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "Dev"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.companyName").exists())
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.errors.appliedDate").exists());
    }

    @Test
    void searchFilterSortAndPaginate() throws Exception {
        TestUser a = newUser();
        createJob(a, "Acme", "Backend", "APPLIED", "2026-01-01", null);
        createJob(a, "Beta", "Frontend", "INTERVIEW", "2026-02-01", null);
        createJob(a, "Acme Labs", "DevOps", "applied", "2026-03-01", null);

        mvc.perform(as(a, get("/api/jobs/search").param("keyword", "ACME")))
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(as(a, get("/api/jobs/search").param("keyword", "devops")))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(as(a, get("/api/jobs/filter").param("status", "Applied")))
                .andExpect(jsonPath("$.length()").value(2));

        // LIKE wildcards are literal
        mvc.perform(as(a, get("/api/jobs/search").param("keyword", "%")))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(as(a, get("/api/jobs/page").param("size", "2").param("sortBy", "companyName").param("direction", "asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme"))
                .andExpect(jsonPath("$.content[1].companyName").value("Acme Labs"));

        mvc.perform(as(a, get("/api/jobs/page").param("sortBy", "appliedDate").param("direction", "desc")))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme Labs"));

        mvc.perform(as(a, get("/api/jobs/page").param("status", "interview").param("keyword", "bet")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void invalidPagingAndSortingParametersAreBadRequests() throws Exception {
        TestUser a = newUser();

        mvc.perform(as(a, get("/api/jobs/page").param("sortBy", "password"))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/page").param("direction", "sideways"))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/page").param("size", "1000"))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/page").param("size", "0"))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/page").param("page", "-1"))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/page").param("page", "x"))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/search").param("keyword", "  "))).andExpect(status().isBadRequest());
        mvc.perform(as(a, get("/api/jobs/search"))).andExpect(status().isBadRequest());
    }

    @Test
    void emptyResultsAreEmptyListsNotErrors() throws Exception {
        TestUser a = newUser();

        mvc.perform(as(a, get("/api/jobs/filter").param("status", "OFFER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
