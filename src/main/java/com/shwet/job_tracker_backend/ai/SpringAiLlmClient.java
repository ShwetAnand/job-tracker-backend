package com.shwet.job_tracker_backend.ai;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.shwet.job_tracker_backend.exception.AiProviderException;
import com.shwet.job_tracker_backend.exception.AiProviderException.Kind;

/**
 * {@link LlmClient} backed by Spring AI's ChatClient (OpenAI by default).
 *
 * Messages are passed as Message objects rather than through .system()/.user() text,
 * because those treat the text as a template: a resume containing "{" or "}" would
 * otherwise be interpreted as a template placeholder.
 */
@Component
public class SpringAiLlmClient implements LlmClient {

    static final String NOT_CONFIGURED_MARKER = "not-configured";

    private final ChatClient chatClient;
    private final boolean configured;

    public SpringAiLlmClient(ChatClient.Builder chatClientBuilder,
                             @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.configured = apiKey != null && !apiKey.isBlank() && !NOT_CONFIGURED_MARKER.equals(apiKey);
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {

        if (!configured) {
            throw new AiProviderException(Kind.NOT_CONFIGURED);
        }

        String content;
        try {
            content = chatClient
                    .prompt(new Prompt(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userPrompt))))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw AiErrorTranslator.translate(e);
        }

        if (content == null || content.isBlank()) {
            throw new AiProviderException(Kind.BAD_RESPONSE);
        }

        return content;
    }
}
