package com.shwet.job_tracker_backend.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClientResponseException;

import com.shwet.job_tracker_backend.exception.AiProviderException;
import com.shwet.job_tracker_backend.exception.AiProviderException.Kind;

class ResumeVectorIndexTest {

    private VectorStore store;
    private ObjectProvider<VectorStore> provider;
    private ResumeVectorIndex index;

    private static RagProperties props(boolean enabled) {
        return new RagProperties(enabled, true, 1536, 200, 40, 5, 0.25, 500);
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        store = mock(VectorStore.class);
        provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(store);
        index = new ResumeVectorIndex(provider, props(true));
    }

    private static Filter.Expression expectedFilter(long userId, long resumeId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return b.and(b.eq("userId", userId), b.eq("resumeId", resumeId)).build();
    }

    private static Document doc(String text, Object userId, Object resumeId, int chunk, double score) {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .text(text)
                .metadata(Map.of("userId", userId, "resumeId", resumeId, "chunkIndex", chunk))
                .score(score)
                .build();
    }

    @Test
    void searchIsFilteredByUserAndResume() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        index.search(1L, 10L, "java experience");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(captor.capture());

        assertThat(captor.getValue().getFilterExpression()).isEqualTo(expectedFilter(1L, 10L));
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.25);
    }

    @Test
    void foreignDocumentsReturnedByTheStoreAreDropped() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                doc("mine, less relevant", 1L, 10L, 1, 0.4),
                doc("someone else's resume", 2L, 99L, 0, 0.99),
                doc("mine, most relevant", 1L, 10L, 0, 0.8)));

        List<RetrievedChunk> result = index.search(1L, 10L, "q");

        assertThat(result).extracting(RetrievedChunk::text)
                .containsExactly("mine, most relevant", "mine, less relevant");
    }

    @Test
    void indexReplacesOldChunksThenStoresNewOnesWithOwnershipMetadata() {
        int stored = index.index(1L, 10L, "word ".repeat(200).trim());

        InOrder order = inOrder(store);
        order.verify(store).delete(expectedFilter(1L, 10L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        order.verify(store).add(captor.capture());

        assertThat(captor.getValue()).hasSize(stored).isNotEmpty();
        assertThat(captor.getValue()).allSatisfy(d -> {
            assertThat(d.getMetadata()).containsEntry("userId", 1L).containsEntry("resumeId", 10L);
            assertThat(d.getText().length()).isLessThanOrEqualTo(200);
        });
    }

    @Test
    void disabledRagFailsClearlyAndDeletesAreNoOps() {
        ResumeVectorIndex disabled = new ResumeVectorIndex(provider, props(false));

        assertThatThrownBy(() -> disabled.search(1L, 10L, "q"))
                .isInstanceOfSatisfying(AiProviderException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.RAG_UNAVAILABLE));

        disabled.deleteForUser(1L);
        disabled.deleteForResume(1L, 10L);
        verifyNoInteractions(store);
    }

    @Test
    void vectorStoreCreationFailureBecomesRagUnavailable() {
        when(provider.getObject()).thenThrow(new IllegalStateException("extension vector not available"));

        assertThatThrownBy(() -> index.search(1L, 10L, "q"))
                .isInstanceOfSatisfying(AiProviderException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.RAG_UNAVAILABLE);
                    assertThat(e.getMessage()).doesNotContain("extension");
                });
    }

    @Test
    void embeddingRateLimitIsTranslated() {
        when(store.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("x", new RestClientResponseException("rate", 429, "x", null, null, null)));

        assertThatThrownBy(() -> index.search(1L, 10L, "q"))
                .isInstanceOfSatisfying(AiProviderException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.RATE_LIMITED));
    }

    @Test
    void deleteFailureDoesNotBlockCaller() {
        doThrow(new IllegalStateException("db down")).when(store).delete(any(Filter.Expression.class));

        index.deleteForUser(1L);   // must not throw
        verify(store).delete(any(Filter.Expression.class));
    }
}
