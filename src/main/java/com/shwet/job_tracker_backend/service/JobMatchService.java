package com.shwet.job_tracker_backend.service;

import com.shwet.job_tracker_backend.dto.JobMatchResponseDto;

public interface JobMatchService {

    /** Both the resume and the job must belong to {@code userId}; otherwise "not found". */
    JobMatchResponseDto matchResumeWithJob(Long userId, Long resumeId, Long jobId);
}
