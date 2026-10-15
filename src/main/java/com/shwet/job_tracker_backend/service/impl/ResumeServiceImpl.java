package com.shwet.job_tracker_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shwet.job_tracker_backend.dto.ResumeResponseDto;
import com.shwet.job_tracker_backend.entity.Resume;
import com.shwet.job_tracker_backend.entity.User;
import com.shwet.job_tracker_backend.exception.ResourceNotFoundException;
import com.shwet.job_tracker_backend.mapper.ResumeMapper;
import com.shwet.job_tracker_backend.rag.ResumeVectorIndex;
import com.shwet.job_tracker_backend.repository.ResumeRepository;
import com.shwet.job_tracker_backend.repository.UserRepository;
import com.shwet.job_tracker_backend.resume.ParsedResume;
import com.shwet.job_tracker_backend.resume.ResumePdfProcessor;
import com.shwet.job_tracker_backend.service.AiResumeService;
import com.shwet.job_tracker_backend.service.ResumeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AiResumeService aiResumeService;
    private final ResumePdfProcessor pdfProcessor;
    private final ResumeVectorIndex vectorIndex;

    // PDF parsing happens before any transaction/DB work; only the save is transactional.
    @Override
    public ResumeResponseDto uploadResume(Long userId, MultipartFile file) {

        ParsedResume parsed = pdfProcessor.process(file);

        return save(userId, parsed);
    }

    private ResumeResponseDto save(Long userId, ParsedResume parsed) {

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resume saved = resumeRepository.save(
                ResumeMapper.toEntity(parsed.fileName(), parsed.text(), owner));

        return ResumeMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDto> listResumes(Long userId) {

        return resumeRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(resume -> ResumeMapper.toDto(resume, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponseDto getResume(Long userId, Long resumeId) {

        return ResumeMapper.toDto(findOwned(userId, resumeId));
    }

    @Override
    @Transactional
    public void deleteResume(Long userId, Long resumeId) {

        Resume resume = findOwned(userId, resumeId);

        vectorIndex.deleteForResume(userId, resumeId);

        resumeRepository.delete(resume);
    }

    // Not transactional on purpose: the LLM call can take seconds and must not hold a DB connection.
    @Override
    public ResumeResponseDto analyzeResume(Long userId, Long resumeId) {

        Resume resume = findOwned(userId, resumeId);

        List<String> skills = aiResumeService.extractSkills(resume.getRawText());

        String extractedSkills = String.join(", ", skills);

        resumeRepository.updateExtractedSkills(resumeId, extractedSkills);

        // Map from the entity we already loaded (user fetched eagerly via the entity graph).
        resume.setExtractedSkills(extractedSkills);
        return ResumeMapper.toDto(resume);
    }

    private Resume findOwned(Long userId, Long resumeId) {

        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }
}
