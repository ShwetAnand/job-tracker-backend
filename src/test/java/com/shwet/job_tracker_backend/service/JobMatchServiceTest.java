package com.shwet.job_tracker_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shwet.job_tracker_backend.dto.JobMatchResponseDto;
import com.shwet.job_tracker_backend.entity.Job;
import com.shwet.job_tracker_backend.entity.Resume;
import com.shwet.job_tracker_backend.entity.User;
import com.shwet.job_tracker_backend.exception.BadRequestException;
import com.shwet.job_tracker_backend.exception.ResourceNotFoundException;
import com.shwet.job_tracker_backend.repository.JobRepository;
import com.shwet.job_tracker_backend.repository.ResumeRepository;
import com.shwet.job_tracker_backend.service.impl.JobMatchServiceImpl;

class JobMatchServiceTest {

    private final ResumeRepository resumeRepository = mock(ResumeRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobMatchServiceImpl service = new JobMatchServiceImpl(resumeRepository, jobRepository);

    private final User owner = User.builder().id(1L).name("Owner").build();

    @BeforeEach
    void setUp() {
        Resume resume = Resume.builder().id(10L).user(owner)
                .extractedSkills("Java, Spring Boot, Docker")
                .rawText("Built REST APIs with Java")
                .build();
        when(resumeRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(resume));
    }

    private void jobWithDescription(String description) {
        Job job = Job.builder().id(20L).user(owner).companyName("Acme").role("Dev").jobDescription(description).build();
        when(jobRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(job));
    }

    @Test
    void computesMatchedMissingAndPercentage() {
        jobWithDescription("Needs Java, Spring Boot, React, REST APIs and Kubernetes");

        JobMatchResponseDto result = service.matchResumeWithJob(1L, 10L, 20L);

        assertThat(result.getMatchedSkills()).containsExactly("java", "rest", "spring");
        assertThat(result.getMissingSkills()).containsExactly("kubernetes", "react");
        assertThat(result.getMatchPercentage()).isEqualTo(60.0);
        assertThat(result.getDisclaimer()).contains("not detected");
    }

    @Test
    void javaScriptRequirementIsNotSatisfiedByJava() {
        jobWithDescription("We need JavaScript");

        JobMatchResponseDto result = service.matchResumeWithJob(1L, 10L, 20L);

        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getMissingSkills()).containsExactly("javascript");
    }

    @Test
    void jobDescriptionWithoutKnownSkillsGivesZeroAndExplanation() {
        jobWithDescription("Friendly team, great snacks");

        JobMatchResponseDto result = service.matchResumeWithJob(1L, 10L, 20L);

        assertThat(result.getMatchPercentage()).isZero();
        assertThat(result.getDisclaimer()).contains("no match percentage");
    }

    @Test
    void blankJobDescriptionIsABadRequest() {
        jobWithDescription("  ");

        assertThatThrownBy(() -> service.matchResumeWithJob(1L, 10L, 20L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void anotherUsersResumeOrJobLooksLikeNotFound() {
        jobWithDescription("Java");

        assertThatThrownBy(() -> service.matchResumeWithJob(2L, 10L, 20L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.matchResumeWithJob(1L, 10L, 999L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
