package com.shwet.job_tracker_backend.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.shwet.job_tracker_backend.dto.ResumeResponseDto;

/** Every method is scoped to {@code userId}; other users' resumes are reported as "not found". */
public interface ResumeService {

    /** Validate the PDF, extract its text and store it for {@code userId}. */
    ResumeResponseDto uploadResume(Long userId, MultipartFile file);

    List<ResumeResponseDto> listResumes(Long userId);

    ResumeResponseDto getResume(Long userId, Long resumeId);

    void deleteResume(Long userId, Long resumeId);

    /** Extract skills with the LLM and store them on the resume. */
    ResumeResponseDto analyzeResume(Long userId, Long resumeId);
}
