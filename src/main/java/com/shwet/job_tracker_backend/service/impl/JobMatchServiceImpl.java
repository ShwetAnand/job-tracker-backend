package com.shwet.job_tracker_backend.service.impl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shwet.job_tracker_backend.dto.JobMatchResponseDto;
import com.shwet.job_tracker_backend.entity.Job;
import com.shwet.job_tracker_backend.entity.Resume;
import com.shwet.job_tracker_backend.exception.BadRequestException;
import com.shwet.job_tracker_backend.exception.ResourceNotFoundException;
import com.shwet.job_tracker_backend.repository.JobRepository;
import com.shwet.job_tracker_backend.repository.ResumeRepository;
import com.shwet.job_tracker_backend.service.JobMatchService;
import com.shwet.job_tracker_backend.skills.SkillCatalog;

import lombok.RequiredArgsConstructor;

/**
 * Deterministic resume / job-description comparison (no LLM call, so it works offline
 * and gives the same answer every time).
 *
 * Required skills = catalog skills found in the job description.
 * Resume skills   = LLM-extracted skills (if the resume was analysed) + catalog skills
 *                   found directly in the resume text.
 */
@Service
@RequiredArgsConstructor
public class JobMatchServiceImpl implements JobMatchService {

    static final String DISCLAIMER =
            "Skills listed as missing were not detected in the resume text. They may still be part of your "
                    + "experience (for example under different wording). Matching only recognises skills "
                    + "from a built-in dictionary.";

    static final String NO_REQUIRED_SKILLS_NOTE =
            " No recognisable skills were found in the job description, so no match percentage could be computed.";

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;

    @Override
    @Transactional(readOnly = true)
    public JobMatchResponseDto matchResumeWithJob(Long userId, Long resumeId, Long jobId) {

        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        Job job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        String jobDescription = job.getJobDescription();

        if (jobDescription == null || jobDescription.isBlank()) {
            throw new BadRequestException("Job description is empty.");
        }

        Set<String> requiredSkills = SkillCatalog.detectIn(jobDescription);
        Set<String> resumeSkills = collectResumeSkills(resume);

        List<String> matchedSkills = requiredSkills.stream()
                .filter(resumeSkills::contains)
                .sorted()
                .toList();

        List<String> missingSkills = requiredSkills.stream()
                .filter(skill -> !resumeSkills.contains(skill))
                .sorted()
                .toList();

        String disclaimer = requiredSkills.isEmpty() ? DISCLAIMER + NO_REQUIRED_SKILLS_NOTE : DISCLAIMER;

        return JobMatchResponseDto.builder()
                .resumeId(resume.getId())
                .jobId(job.getId())
                .companyName(job.getCompanyName())
                .role(job.getRole())
                .matchPercentage(percentage(matchedSkills.size(), requiredSkills.size()))
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .disclaimer(disclaimer)
                .build();
    }

    private Set<String> collectResumeSkills(Resume resume) {

        Set<String> skills = new LinkedHashSet<>();

        String extracted = resume.getExtractedSkills();
        if (extracted != null && !extracted.isBlank()) {
            skills.addAll(SkillCatalog.canonicalizeAll(Arrays.asList(extracted.split(","))));
        }

        skills.addAll(SkillCatalog.detectIn(resume.getRawText()));

        return skills;
    }

    private double percentage(int matched, int required) {

        if (required == 0) {
            return 0.0;
        }

        return Math.round(((double) matched / required) * 10000.0) / 100.0;
    }
}
