package com.shwet.job_tracker_backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * OPTIONAL live test against the real OpenAI API. Skipped automatically unless OPENAI_API_KEY is set:
 *
 *   OPENAI_API_KEY=sk-... mvn test -Dtest=LiveAiSmokeTest
 *
 * It only exercises the chat path (no database / pgvector needed). It costs a few tokens.
 */
@Tag("live-ai")
@SpringBootTest(properties = "spring.ai.openai.api-key=${OPENAI_API_KEY}")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class LiveAiSmokeTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    void realModelAnswers() {
        String answer = llmClient.complete("Reply with exactly one word.", "Say: pong");

        assertThat(answer).isNotBlank();
    }
}
