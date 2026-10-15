package com.shwet.job_tracker_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shwet.job_tracker_backend.dto.JobMatchResponseDto;
import com.shwet.job_tracker_backend.security.CustomUserDetails;
import com.shwet.job_tracker_backend.service.JobMatchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/job-match")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @PostMapping("/resume/{resumeId}/job/{jobId}")
    public ResponseEntity<JobMatchResponseDto> matchResumeWithJob(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                jobMatchService.matchResumeWithJob(principal.getId(), resumeId, jobId));
    }
}
