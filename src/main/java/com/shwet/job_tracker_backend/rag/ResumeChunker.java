package com.shwet.job_tracker_backend.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits text into overlapping chunks of at most {@code chunkSize} characters.
 * Chunk boundaries are moved back to a newline or space where possible so words are not cut in half,
 * and consecutive chunks share {@code overlap} characters so a fact on a boundary is still retrievable.
 *
 * A plain character-based splitter is used deliberately: it is deterministic, has no tokenizer
 * dependency and is trivial to unit test. The defaults (about 1000 characters, roughly 250 tokens)
 * keep each chunk focused on one resume section.
 */
public final class ResumeChunker {

    private ResumeChunker() {
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {

        if (chunkSize < 100) {
            throw new IllegalArgumentException("chunkSize must be at least 100");
        }
        if (overlap < 0 || overlap >= chunkSize / 2) {
            throw new IllegalArgumentException("overlap must be >= 0 and less than half of chunkSize");
        }

        List<String> chunks = new ArrayList<>();

        if (text == null) {
            return chunks;
        }

        String normalized = text.replace("\r\n", "\n").trim();

        int length = normalized.length();
        int start = 0;

        while (start < length) {

            int end = Math.min(start + chunkSize, length);

            if (end < length) {
                end = findBoundary(normalized, start + chunkSize / 2, end);
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= length) {
                break;
            }

            // end - start >= chunkSize/2 > overlap, so start strictly increases
            start = end - overlap;
        }

        return chunks;
    }

    /** Last newline (preferred) or space in [minEnd, maxEnd); otherwise maxEnd. */
    private static int findBoundary(String text, int minEnd, int maxEnd) {

        for (int i = maxEnd; i > minEnd; i--) {
            if (text.charAt(i - 1) == '\n') {
                return i;
            }
        }
        for (int i = maxEnd; i > minEnd; i--) {
            if (text.charAt(i - 1) == ' ') {
                return i;
            }
        }
        return maxEnd;
    }
}
