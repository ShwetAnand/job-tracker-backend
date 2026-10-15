package com.shwet.job_tracker_backend.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shwet.job_tracker_backend.dto.JobPageQuery;
import com.shwet.job_tracker_backend.dto.JobRequestDto;
import com.shwet.job_tracker_backend.dto.JobResponseDto;
import com.shwet.job_tracker_backend.entity.Job;
import com.shwet.job_tracker_backend.entity.User;
import com.shwet.job_tracker_backend.exception.BadRequestException;
import com.shwet.job_tracker_backend.exception.ResourceNotFoundException;
import com.shwet.job_tracker_backend.mapper.JobMapper;
import com.shwet.job_tracker_backend.repository.JobRepository;
import com.shwet.job_tracker_backend.repository.UserRepository;
import com.shwet.job_tracker_backend.repository.spec.JobSpecifications;
import com.shwet.job_tracker_backend.service.JobService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    /** Only these properties may be used for sorting (prevents sorting on arbitrary/internal fields). */
    static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "companyName", "role", "status", "appliedDate");

    static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public JobResponseDto addJob(Long userId, JobRequestDto requestDto) {

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Job job = JobMapper.toEntity(requestDto, owner);

        return JobMapper.toDto(jobRepository.save(job));
    }

    @Override
    public List<JobResponseDto> getAllJobs(Long userId) {

        Sort sort = Sort.by(Sort.Order.desc("appliedDate"), Sort.Order.desc("id"));

        return jobRepository.findAll(JobSpecifications.ownedBy(userId), sort)
                .stream()
                .map(JobMapper::toDto)
                .toList();
    }

    @Override
    public JobResponseDto getJobById(Long userId, Long id) {

        return JobMapper.toDto(findOwnedJob(userId, id));
    }

    @Override
    @Transactional
    public JobResponseDto updateJob(Long userId, Long id, JobRequestDto dto) {

        Job job = findOwnedJob(userId, id);

        job.setCompanyName(dto.getCompanyName());
        job.setRole(dto.getRole());
        job.setStatus(dto.getStatus());
        job.setAppliedDate(dto.getAppliedDate());
        job.setNotes(dto.getNotes());
        job.setJobDescription(dto.getJobDescription());
        // The owner is never changed by an update.

        return JobMapper.toDto(jobRepository.save(job));
    }

    @Override
    @Transactional
    public void deleteJob(Long userId, Long id) {

        jobRepository.delete(findOwnedJob(userId, id));
    }

    @Override
    public List<JobResponseDto> searchJobs(Long userId, String keyword) {

        requireText(keyword, "keyword");

        Specification<Job> spec = JobSpecifications.ownedBy(userId)
                .and(JobSpecifications.keywordMatches(keyword));

        return jobRepository.findAll(spec, Sort.by(Sort.Order.desc("appliedDate"), Sort.Order.desc("id")))
                .stream()
                .map(JobMapper::toDto)
                .toList();
    }

    @Override
    public List<JobResponseDto> filterJobs(Long userId, String status) {

        requireText(status, "status");

        Specification<Job> spec = JobSpecifications.ownedBy(userId)
                .and(JobSpecifications.statusEquals(status));

        return jobRepository.findAll(spec, Sort.by(Sort.Order.desc("appliedDate"), Sort.Order.desc("id")))
                .stream()
                .map(JobMapper::toDto)
                .toList();
    }

    @Override
    public Page<JobResponseDto> getJobs(Long userId, JobPageQuery query) {

        if (query.page() < 0) {
            throw new BadRequestException("page must be 0 or greater");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (query.sortBy() == null || !SORTABLE_FIELDS.contains(query.sortBy())) {
            throw new BadRequestException("sortBy must be one of " + SORTABLE_FIELDS.stream().sorted().toList());
        }

        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(query.direction())) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(query.direction())) {
            direction = Sort.Direction.DESC;
        } else {
            throw new BadRequestException("direction must be 'asc' or 'desc'");
        }

        // Secondary sort on id keeps page boundaries stable when the primary key has ties.
        Sort sort = Sort.by(direction, query.sortBy()).and(Sort.by(Sort.Direction.ASC, "id"));

        Specification<Job> spec = JobSpecifications.ownedBy(userId);

        if (query.keyword() != null && !query.keyword().isBlank()) {
            spec = spec.and(JobSpecifications.keywordMatches(query.keyword()));
        }
        if (query.status() != null && !query.status().isBlank()) {
            spec = spec.and(JobSpecifications.statusEquals(query.status()));
        }

        return jobRepository
                .findAll(spec, PageRequest.of(query.page(), query.size(), sort))
                .map(JobMapper::toDto);
    }

    private Job findOwnedJob(Long userId, Long id) {

        return jobRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(name + " must not be blank");
        }
    }
}
