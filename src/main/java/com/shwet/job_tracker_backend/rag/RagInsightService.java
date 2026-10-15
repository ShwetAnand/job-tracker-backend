package com.shwet.job_tracker_backend.rag;

import com.shwet.job_tracker_backend.dto.InsightRequestDto;
import com.shwet.job_tracker_backend.dto.InsightResponseDto;

public interface RagInsightService {

    /**
     * Retrieval-augmented resume insights for one of the user's resumes.
     * Steps: ownership checks -> (re)index resume chunks -> similarity search scoped to this user and
     * resume -> prompt with ONLY the retrieved chunks -> validated JSON answer.
     */
    InsightResponseDto generateInsights(Long userId, Long resumeId, InsightRequestDto request);
}
