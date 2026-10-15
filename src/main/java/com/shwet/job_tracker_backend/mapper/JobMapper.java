package com.shwet.job_tracker_backend.mapper;

import com.shwet.job_tracker_backend.dto.JobRequestDto;
import com.shwet.job_tracker_backend.dto.JobResponseDto;
import com.shwet.job_tracker_backend.entity.Job;
import com.shwet.job_tracker_backend.entity.User;

public class JobMapper {

    public static Job toEntity(JobRequestDto dto, User user) {

        return Job.builder()
                .companyName(dto.getCompanyName())
                .role(dto.getRole())
                .status(dto.getStatus())
                .appliedDate(dto.getAppliedDate())
                .notes(dto.getNotes())
                .jobDescription(dto.getJobDescription())
                .user(user)
                .build();
    }

    public static JobResponseDto toDto(Job job) {

        return JobResponseDto.builder()
                .id(job.getId())
                .companyName(job.getCompanyName())
                .role(job.getRole())
                .status(job.getStatus())
                .appliedDate(job.getAppliedDate())
                .notes(job.getNotes())
                .jobDescription(job.getJobDescription())
                .userId(job.getUser().getId())
                .userName(job.getUser().getName())
                .build();
    }
}