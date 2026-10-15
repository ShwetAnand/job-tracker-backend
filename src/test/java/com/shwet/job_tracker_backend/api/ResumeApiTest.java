package com.shwet.job_tracker_backend.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.shwet.job_tracker_backend.ai.LlmClient;
import com.shwet.job_tracker_backend.exception.AiProviderException;
import com.shwet.job_tracker_backend.exception.AiProviderException.Kind;
import com.shwet.job_tracker_backend.rag.ResumeVectorIndex;
import com.shwet.job_tracker_backend.rag.RetrievedChunk;
import com.shwet.job_tracker_backend.testsupport.PdfTestUtil;

/**
 * External AI calls (LLM, embeddings/vector store) are mocked; everything else
 * is real.
 */
class ResumeApiTest extends AbstractApiTest {

        @MockitoBean
        private LlmClient llmClient;

        @MockitoBean
        private ResumeVectorIndex vectorIndex;

        private MockMultipartFile pdf() {
                return new MockMultipartFile("file", "resume.pdf", "application/pdf",
                                PdfTestUtil.pdfWithText(PdfTestUtil.SAMPLE_RESUME_TEXT));
        }

        private long upload(TestUser user) throws Exception {
                return body(mvc.perform(as(user, multipart("/api/resumes/upload").file(pdf()))))
                                .get("id").asLong();
        }

        @Test
        void uploadStoresResumeForTheAuthenticatedUserAndIgnoresClientUserId() throws Exception {
                TestUser a = newUser();
                TestUser b = newUser();

                mvc.perform(as(a, multipart("/api/resumes/upload").file(pdf()).param("userId", String.valueOf(b.id()))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.userId").value(a.id()))
                                .andExpect(jsonPath("$.rawText", containsString("Jane Doe")));

                mvc.perform(as(b, get("/api/resumes"))).andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void uploadRequiresAuthentication() throws Exception {
                mvc.perform(multipart("/api/resumes/upload").file(pdf())).andExpect(status().isUnauthorized());
        }

        @Test
        void uploadValidation() throws Exception {
                TestUser a = newUser();

                mvc.perform(as(a, multipart("/api/resumes/upload")
                                .file(new MockMultipartFile("file", "notes.txt", "text/plain",
                                                "hello".getBytes(StandardCharsets.UTF_8)))))
                                .andExpect(status().isUnsupportedMediaType());

                mvc.perform(as(a, multipart("/api/resumes/upload")
                                .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]))))
                                .andExpect(status().isBadRequest());

                mvc.perform(as(a, multipart("/api/resumes/upload")
                                .file(new MockMultipartFile("file", "fake.pdf", "application/pdf",
                                                "not really a pdf".getBytes(StandardCharsets.UTF_8)))))
                                .andExpect(status().isUnsupportedMediaType());

                mvc.perform(as(a, multipart("/api/resumes/upload")
                                .file(new MockMultipartFile("file", "scan.pdf", "application/pdf",
                                                PdfTestUtil.pdf(1, "")))))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("Scanned")));

                byte[] huge = new byte[6 * 1024 * 1024];
                System.arraycopy("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), 0, huge, 0, 8);
                mvc.perform(as(a, multipart("/api/resumes/upload")
                                .file(new MockMultipartFile("file", "huge.pdf", "application/pdf", huge))))
                                .andExpect(status().isPayloadTooLarge());

                mvc.perform(as(a, multipart("/api/resumes/upload")))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", containsString("'file'")));
        }

        @Test
        void listOmitsTextAndGetIncludesIt() throws Exception {
                TestUser a = newUser();
                long id = upload(a);

                mvc.perform(as(a, get("/api/resumes")))
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].rawText").doesNotExist());

                mvc.perform(as(a, get("/api/resumes/" + id)))
                                .andExpect(jsonPath("$.rawText", containsString("Spring Boot")));
        }

        @Test
        void userBCannotTouchUserAsResume() throws Exception {
                TestUser a = newUser();
                TestUser b = newUser();
                long resumeId = upload(a);
                long jobId = createJob(b, "BCorp", "Dev", "APPLIED", "2026-01-01", "Needs Java");

                mvc.perform(as(b, get("/api/resumes/" + resumeId))).andExpect(status().isNotFound());
                mvc.perform(as(b, post("/api/resumes/" + resumeId + "/analyze"))).andExpect(status().isNotFound());
                mvc.perform(as(b, post("/api/resumes/" + resumeId + "/insights"))).andExpect(status().isNotFound());
                mvc.perform(as(b, post("/api/job-match/resume/" + resumeId + "/job/" + jobId)))
                                .andExpect(status().isNotFound());
                mvc.perform(as(b, delete("/api/resumes/" + resumeId))).andExpect(status().isNotFound());

                // the LLM and vector store were never reached for the foreign resume
                org.mockito.Mockito.verifyNoInteractions(llmClient);
                org.mockito.Mockito.verify(vectorIndex, org.mockito.Mockito.never()).search(anyLong(), anyLong(),
                                anyString());

                mvc.perform(as(a, get("/api/resumes/" + resumeId))).andExpect(status().isOk());
        }

        @Test
        void analyzeStoresSkillsReturnedByTheModel() throws Exception {
                TestUser a = newUser();
                long id = upload(a);
                when(llmClient.complete(anyString(), anyString())).thenReturn("Java, Spring Boot, Docker");

                mvc.perform(as(a, post("/api/resumes/" + id + "/analyze")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.extractedSkills").value("Java, Spring Boot, Docker"));

                mvc.perform(as(a, get("/api/resumes/" + id)))
                                .andExpect(jsonPath("$.extractedSkills").value("Java, Spring Boot, Docker"));
        }

        @Test
        void aiProviderFailuresMapToSafeStatusCodes() throws Exception {
                TestUser a = newUser();
                long id = upload(a);

                // doThrow/doReturn: re-stubbing with when(...) would invoke the previously
                // stubbed exception
                org.mockito.Mockito.doThrow(new AiProviderException(Kind.RATE_LIMITED))
                                .when(llmClient).complete(anyString(), anyString());
                mvc.perform(as(a, post("/api/resumes/" + id + "/analyze")))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.message", not(containsString("sk-"))));

                org.mockito.Mockito.doThrow(new AiProviderException(Kind.TIMEOUT))
                                .when(llmClient).complete(anyString(), anyString());
                mvc.perform(as(a, post("/api/resumes/" + id + "/analyze"))).andExpect(status().isGatewayTimeout());

                org.mockito.Mockito.doThrow(new AiProviderException(Kind.NOT_CONFIGURED))
                                .when(llmClient).complete(anyString(), anyString());
                mvc.perform(as(a, post("/api/resumes/" + id + "/analyze"))).andExpect(status().isServiceUnavailable());

                org.mockito.Mockito.doReturn("   ,  , ")
                                .when(llmClient).complete(anyString(), anyString());
                mvc.perform(as(a, post("/api/resumes/" + id + "/analyze"))).andExpect(status().isBadGateway());
        }

        @Test
        void jobMatchComparesResumeWithJobDescription() throws Exception {
                TestUser a = newUser();
                long resumeId = upload(a);
                long jobId = createJob(
                                a,
                                "Acme",
                                "Dev",
                                "APPLIED",
                                "2026-01-01",
                                "Java, Spring Boot, Kubernetes and React");

                mvc.perform(as(a, post(
                                "/api/job-match/resume/" + resumeId + "/job/" + jobId)))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.matchedSkills").isArray())
                                .andExpect(jsonPath("$.matchedSkills[0]").value("java"))
                                .andExpect(jsonPath("$.matchedSkills[1]").value("spring"))
                                .andExpect(jsonPath("$.missingSkills[0]").value("kubernetes"))
                                .andExpect(jsonPath("$.missingSkills[1]").value("react"))
                                .andExpect(jsonPath("$.matchPercentage").value(50.0))
                                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
        }

        @Test
        void insightsUseRetrievedChunksAndReturnSources() throws Exception {
                TestUser a = newUser();
                long id = upload(a);
                when(vectorIndex.search(eq(a.id()), eq(id), anyString()))
                                .thenReturn(List.of(new RetrievedChunk(0, "Built microservices with JWT", 0.8)));
                when(llmClient.complete(anyString(), anyString())).thenReturn(
                                "{\"summary\":\"Good\",\"strengths\":[\"JWT\"],\"gaps\":[],\"recommendations\":[\"Add tests\"]}");

                mvc.perform(as(a, post("/api/resumes/" + id + "/insights")).contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("question", "What am I good at?"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.grounded").value(true))
                                .andExpect(jsonPath("$.sources.length()").value(1))
                                .andExpect(jsonPath("$.strengths[0]").value("JWT"));

                verify(vectorIndex).index(eq(a.id()), eq(id), any());
        }

        @Test
        void insightsReportUnavailableRetrievalAndTooLongQuestions() throws Exception {
                TestUser a = newUser();
                long id = upload(a);

                org.mockito.Mockito.doThrow(new AiProviderException(Kind.RAG_UNAVAILABLE))
                                .when(vectorIndex).index(anyLong(), anyLong(), any());
                mvc.perform(as(a, post("/api/resumes/" + id + "/insights"))).andExpect(status().isServiceUnavailable());

                mvc.perform(as(a, post("/api/resumes/" + id + "/insights")).contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("question", "x".repeat(501)))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.question").exists());
        }

        @Test
        void deleteRemovesResumeAndItsEmbeddings() throws Exception {
                TestUser a = newUser();
                long id = upload(a);

                mvc.perform(as(a, delete("/api/resumes/" + id))).andExpect(status().isOk());

                verify(vectorIndex).deleteForResume(a.id(), id);
                mvc.perform(as(a, get("/api/resumes/" + id))).andExpect(status().isNotFound());
        }
}
