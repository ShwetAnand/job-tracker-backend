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
public class JobMatchResponseDto {

    private Long resumeId;

    private Long jobId;

    private String companyName;

    private String role;

    private double matchPercentage;

    private List<String> matchedSkills;

    /**
     * Skills the job description asks for that were NOT DETECTED in the resume.
     * (Name kept for API compatibility.) This is not a statement that the candidate lacks the skill.
     */
    private List<String> missingSkills;

    /** Plain-language caveat the UI should show next to the result. */
    private String disclaimer;
}
