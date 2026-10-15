package com.shwet.job_tracker_backend.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.shwet.job_tracker_backend.dto.JobPageQuery;
import com.shwet.job_tracker_backend.dto.JobRequestDto;
import com.shwet.job_tracker_backend.dto.JobResponseDto;

/**
 * Every method is scoped to {@code userId} (the authenticated user, resolved server-side).
 * A job that exists but belongs to someone else is reported exactly like a missing one.
 */
public interface JobService {

    JobResponseDto addJob(Long userId, JobRequestDto requestDto);

    List<JobResponseDto> getAllJobs(Long userId);

    JobResponseDto getJobById(Long userId, Long id);

    JobResponseDto updateJob(Long userId, Long id, JobRequestDto requestDto);

    void deleteJob(Long userId, Long id);

    List<JobResponseDto> searchJobs(Long userId, String keyword);

    List<JobResponseDto> filterJobs(Long userId, String status);

    Page<JobResponseDto> getJobs(Long userId, JobPageQuery query);
}
