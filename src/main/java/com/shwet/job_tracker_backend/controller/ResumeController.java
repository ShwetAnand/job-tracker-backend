package com.shwet.job_tracker_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shwet.job_tracker_backend.dto.InsightRequestDto;
import com.shwet.job_tracker_backend.dto.InsightResponseDto;
import com.shwet.job_tracker_backend.dto.ResumeResponseDto;
import com.shwet.job_tracker_backend.rag.RagInsightService;
import com.shwet.job_tracker_backend.security.CustomUserDetails;
import com.shwet.job_tracker_backend.service.ResumeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** All endpoints operate on the authenticated user's resumes only. */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final RagInsightService ragInsightService;

    /**
     * Upload a PDF resume. The owner is the authenticated user.
     * (The former "userId" form field is no longer read; clients that still send it are unaffected.)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponseDto> uploadResume(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file) {

        return new ResponseEntity<>(
                resumeService.uploadResume(principal.getId(), file),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponseDto>> listResumes(
            @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(resumeService.listResumes(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponseDto> getResume(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        return ResponseEntity.ok(resumeService.getResume(principal.getId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteResume(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        resumeService.deleteResume(principal.getId(), id);
        return ResponseEntity.ok("Resume Deleted Successfully");
    }

    /** LLM skill extraction. */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<ResumeResponseDto> analyzeResume(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        return ResponseEntity.ok(resumeService.analyzeResume(principal.getId(), id));
    }

    /** RAG: retrieval-augmented insights / career recommendations for this resume. */
    @PostMapping("/{id}/insights")
    public ResponseEntity<InsightResponseDto> insights(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) InsightRequestDto request) {

        return ResponseEntity.ok(ragInsightService.generateInsights(principal.getId(), id, request));
    }
}
