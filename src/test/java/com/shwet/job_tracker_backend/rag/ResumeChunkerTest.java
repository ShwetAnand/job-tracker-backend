package com.shwet.job_tracker_backend.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResumeChunkerTest {

    @Test
    void shortTextIsOneChunk() {
        assertThat(ResumeChunker.chunk("Hello resume", 1000, 100)).containsExactly("Hello resume");
    }

    @Test
    void longTextIsSplitWithinSizeAndOverlaps() {
        String text = "word ".repeat(1000).trim();
        List<String> chunks = ResumeChunker.chunk(text, 500, 100);

        assertThat(chunks.size()).isGreaterThan(5);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(500));
        assertThat(text).endsWith(chunks.get(chunks.size() - 1));
    }

    @Test
    void prefersNewlineBoundaries() {
        String text = "a".repeat(300) + "\n" + "b".repeat(300);
        List<String> chunks = ResumeChunker.chunk(text, 400, 50);

        assertThat(chunks.get(0)).isEqualTo("a".repeat(300));
    }

    @Test
    void emptyInputGivesNoChunks() {
        assertThat(ResumeChunker.chunk("   ", 1000, 100)).isEmpty();
        assertThat(ResumeChunker.chunk(null, 1000, 100)).isEmpty();
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThatThrownBy(() -> ResumeChunker.chunk("x", 50, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResumeChunker.chunk("x", 200, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}
