package com.shwet.job_tracker_backend.ai;

import com.shwet.job_tracker_backend.exception.AiProviderException;

/**
 * Minimal port to the chat model. Business services depend on this interface rather
 * than on Spring AI types, which keeps them easy to unit test with a mock.
 */
public interface LlmClient {

    /**
     * @return the model's text answer (never null/blank)
     * @throws AiProviderException for configuration, rate-limit, timeout, provider or empty-response problems
     */
    String complete(String systemPrompt, String userPrompt);
}
