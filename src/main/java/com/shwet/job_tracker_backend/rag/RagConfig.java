package com.shwet.job_tracker_backend.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Builds the pgvector-backed {@link VectorStore} on top of the application's own PostgreSQL
 * datasource and the OpenAI embedding model.
 *
 * The bean is @Lazy on purpose: PgVectorStore runs "CREATE EXTENSION vector" when it is
 * created, which fails on a PostgreSQL server without pgvector. Creating it on first use
 * keeps the rest of the application (jobs, auth, resumes) bootable and turns the problem into a
 * clear 503 on the RAG endpoint instead of a startup crash.
 *
 * Disable completely with app.rag.enabled=false (RAG_ENABLED=false).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagConfig {

    @Bean
    @Lazy
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate,
                                   EmbeddingModel embeddingModel,
                                   RagProperties properties) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(properties.embeddingDimensions())
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(properties.initializeSchema())
                .build();
    }
}
