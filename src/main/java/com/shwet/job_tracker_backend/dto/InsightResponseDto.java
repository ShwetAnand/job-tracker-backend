package com.shwet.job_tracker_backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightResponseDto {

    private Long resumeId;

    private Long jobId;

    private String question;

    /** false when nothing relevant was retrieved - in that case no LLM answer was generated. */
    private boolean grounded;

    private String summary;

    private List<String> strengths;

    /** Areas where the retrieved resume passages do not show evidence - not proof of missing ability. */
    private List<String> gaps;

    private List<String> recommendations;

    /** The retrieved resume passages the answer is based on. */
    private List<Source> sources;

    private String disclaimer;

    public record Source(int chunkIndex, String snippet, Double score) {
    }
}
