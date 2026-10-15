package com.shwet.job_tracker_backend.mapper;

import java.time.LocalDateTime;

import com.shwet.job_tracker_backend.dto.ResumeResponseDto;
import com.shwet.job_tracker_backend.entity.Resume;
import com.shwet.job_tracker_backend.entity.User;

public class ResumeMapper {

    private ResumeMapper() {
        // Utility class
    }

    public static Resume toEntity(String fileName, String rawText, User user) {

        return Resume.builder()
                .fileName(fileName)
                .rawText(rawText)
                .uploadedAt(LocalDateTime.now())
                .user(user)
                .build();
    }

    public static ResumeResponseDto toDto(Resume resume) {
        return toDto(resume, true);
    }

    public static ResumeResponseDto toDto(Resume resume, boolean includeText) {

        return ResumeResponseDto.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .rawText(includeText ? resume.getRawText() : null)
                .extractedSkills(resume.getExtractedSkills())
                .uploadedAt(resume.getUploadedAt())
                .indexedAt(resume.getIndexedAt())
                .userId(resume.getUser().getId())
                .userName(resume.getUser().getName())
                .build();
    }
}
