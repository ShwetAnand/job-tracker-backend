package com.shwet.job_tracker_backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shwet.job_tracker_backend.dto.JobPageQuery;
import com.shwet.job_tracker_backend.dto.JobRequestDto;
import com.shwet.job_tracker_backend.dto.JobResponseDto;
import com.shwet.job_tracker_backend.security.CustomUserDetails;
import com.shwet.job_tracker_backend.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** All endpoints operate on the authenticated user's jobs only. */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponseDto> addJob(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody JobRequestDto requestDto) {

        return new ResponseEntity<>(jobService.addJob(principal.getId(), requestDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<JobResponseDto>> getAllJobs(
            @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(jobService.getAllJobs(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        return ResponseEntity.ok(jobService.getJobById(principal.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDto> updateJob(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody JobRequestDto requestDto) {

        return ResponseEntity.ok(jobService.updateJob(principal.getId(), id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteJob(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        jobService.deleteJob(principal.getId(), id);
        return ResponseEntity.ok("Job Deleted Successfully");
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobResponseDto>> searchJobs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String keyword) {

        return ResponseEntity.ok(jobService.searchJobs(principal.getId(), keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<JobResponseDto>> filterJobs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String status) {

        return ResponseEntity.ok(jobService.filterJobs(principal.getId(), status));
    }

    /** Pagination + sorting, with optional keyword/status filters. */
    @GetMapping("/page")
    public ResponseEntity<Page<JobResponseDto>> getJobs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "companyName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(jobService.getJobs(
                principal.getId(),
                new JobPageQuery(page, size, sortBy, direction, keyword, status)));
    }
}
