package com.shwet.job_tracker_backend.rag;

/** One retrieved resume passage. {@code score} is the similarity (higher = closer), may be null. */
public record RetrievedChunk(int chunkIndex, String text, Double score) {
}
