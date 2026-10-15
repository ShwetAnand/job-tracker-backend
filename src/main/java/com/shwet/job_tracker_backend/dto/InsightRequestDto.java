package com.shwet.job_tracker_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsightRequestDto {

    /** What the user wants to know. Optional - a general strengths/gaps question is used when absent. */
    @Size(max = 500, message = "Question must be at most 500 characters")
    private String question;

    /** Optional: one of the caller's own tracked jobs; its description steers retrieval and the answer. */
    private Long jobId;

    /** Re-embed the resume before answering (use after changing the embedding model/dimensions). */
    private boolean reindex;
}
