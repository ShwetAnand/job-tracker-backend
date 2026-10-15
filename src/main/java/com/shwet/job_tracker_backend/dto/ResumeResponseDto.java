package com.shwet.job_tracker_backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeResponseDto {

    private Long id;

    private String fileName;

    /** Full extracted text. Present on upload/get/analyze responses, omitted from the list endpoint. */
    private String rawText;

    private String extractedSkills;

    private LocalDateTime uploadedAt;

    /** Set once the resume has been embedded for retrieval (RAG); absent before that. */
    private LocalDateTime indexedAt;

    private Long userId;

    private String userName;
}
