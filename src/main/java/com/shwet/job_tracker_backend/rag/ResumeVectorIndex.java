package com.shwet.job_tracker_backend.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.shwet.job_tracker_backend.ai.AiErrorTranslator;
import com.shwet.job_tracker_backend.exception.AiProviderException;
import com.shwet.job_tracker_backend.exception.AiProviderException.Kind;

import lombok.RequiredArgsConstructor;

/**
 * The only class that talks to the vector store. It enforces per-user isolation:
 *
 * <ul>
 *   <li>every chunk is stored with {@code userId} and {@code resumeId} metadata;</li>
 *   <li>every search is filtered by BOTH {@code userId} and {@code resumeId};</li>
 *   <li>results are re-checked in memory and anything with a different userId is dropped
 *       (defence in depth against a filter bug).</li>
 * </ul>
 *
 * Callers must additionally verify in the relational database that the resume belongs to the user
 * (RagInsightServiceImpl does).
 */
@Component
@RequiredArgsConstructor
public class ResumeVectorIndex {

    private static final Logger log = LoggerFactory.getLogger(ResumeVectorIndex.class);

    static final String USER_ID = "userId";
    static final String RESUME_ID = "resumeId";
    static final String CHUNK_INDEX = "chunkIndex";

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;

    /** Replaces any existing chunks of the resume. @return number of chunks stored */
    public int index(Long userId, Long resumeId, String resumeText) {

        VectorStore store = store();

        List<String> chunks = ResumeChunker.chunk(resumeText, properties.chunkSize(), properties.chunkOverlap());

        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = Map.of(
                    USER_ID, userId,
                    RESUME_ID, resumeId,
                    CHUNK_INDEX, i,
                    "type", "resume");
            documents.add(new Document(chunks.get(i), metadata));
        }

        try {
            store.delete(resumeFilter(userId, resumeId));
            if (!documents.isEmpty()) {
                store.add(documents);   // computes the embeddings (provider call)
            }
        } catch (RuntimeException e) {
            throw AiErrorTranslator.translate(e);
        }

        return documents.size();
    }

    /** Most relevant chunks of ONE resume of ONE user for the query, best first. */
    public List<RetrievedChunk> search(Long userId, Long resumeId, String query) {

        VectorStore store = store();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .filterExpression(resumeFilter(userId, resumeId))
                .build();

        List<Document> hits;
        try {
            hits = store.similaritySearch(request);   // embeds the query (provider call)
        } catch (RuntimeException e) {
            throw AiErrorTranslator.translate(e);
        }

        if (hits == null) {
            return List.of();
        }

        return hits.stream()
                .filter(doc -> belongsTo(doc, userId, resumeId))
                .map(doc -> new RetrievedChunk(chunkIndexOf(doc), doc.getText(), doc.getScore()))
                .sorted(Comparator.comparing(
                        (RetrievedChunk c) -> c.score() == null ? 0.0 : c.score()).reversed())
                .toList();
    }

    public void deleteForResume(Long userId, Long resumeId) {
        deleteQuietly(resumeFilter(userId, resumeId), "resume " + resumeId);
    }

    public void deleteForUser(Long userId) {
        deleteQuietly(new FilterExpressionBuilder().eq(USER_ID, userId).build(), "user " + userId);
    }

    /**
     * Deletion must never block removing the relational data: when RAG is disabled there is nothing to
     * delete, and when the store is unavailable no vectors could have been written. Failures are logged
     * (not swallowed silently).
     */
    private void deleteQuietly(Filter.Expression filter, String what) {

        if (!properties.enabled()) {
            return;
        }

        try {
            VectorStore store = vectorStoreProvider.getObject();
            store.delete(filter);
        } catch (RuntimeException e) {
            log.warn("Could not delete embeddings for {}: {}", what, e.getClass().getSimpleName());
        }
    }

    private VectorStore store() {

        if (!properties.enabled()) {
            throw new AiProviderException(Kind.RAG_UNAVAILABLE);
        }

        try {
            return vectorStoreProvider.getObject();
        } catch (RuntimeException e) {
            log.warn("Vector store could not be created: {}", e.getClass().getSimpleName());
            throw new AiProviderException(Kind.RAG_UNAVAILABLE, e);
        }
    }

    private static Filter.Expression resumeFilter(Long userId, Long resumeId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return b.and(b.eq(USER_ID, userId), b.eq(RESUME_ID, resumeId)).build();
    }

    private static boolean belongsTo(Document doc, Long userId, Long resumeId) {
        Map<String, Object> metadata = doc.getMetadata();
        return metadata != null
                && Objects.equals(String.valueOf(metadata.get(USER_ID)), String.valueOf(userId))
                && Objects.equals(String.valueOf(metadata.get(RESUME_ID)), String.valueOf(resumeId));
    }

    private static int chunkIndexOf(Document doc) {
        Object value = doc.getMetadata().get(CHUNK_INDEX);
        return value instanceof Number n ? n.intValue() : -1;
    }
}
