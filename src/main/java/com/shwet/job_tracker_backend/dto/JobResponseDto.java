package com.shwet.job_tracker_backend.dto;

import java.time.LocalDate;

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
public class JobResponseDto {

    private Long id;

    private String companyName;

    private String role;

    private String status;

    private LocalDate appliedDate;

    private String notes;

    // Job description for AI analysis
    private String jobDescription;

    private Long userId;

    private String userName;
}