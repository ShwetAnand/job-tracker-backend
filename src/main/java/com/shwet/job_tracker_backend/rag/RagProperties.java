package com.shwet.job_tracker_backend.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** RAG settings (app.rag.*). */
@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("true") boolean initializeSchema,
        @DefaultValue("1536") int embeddingDimensions,
        @DefaultValue("1000") int chunkSize,
        @DefaultValue("150") int chunkOverlap,
        @DefaultValue("5") int topK,
        @DefaultValue("0.25") double similarityThreshold,
        @DefaultValue("500") int maxQuestionChars) {
}
