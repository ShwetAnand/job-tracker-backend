package com.shwet.job_tracker_backend.service.impl;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shwet.job_tracker_backend.dto.UserRequestDto;
import com.shwet.job_tracker_backend.dto.UserResponseDto;
import com.shwet.job_tracker_backend.entity.User;
import com.shwet.job_tracker_backend.exception.EmailAlreadyExistsException;
import com.shwet.job_tracker_backend.exception.ResourceNotFoundException;
import com.shwet.job_tracker_backend.mapper.UserMapper;
import com.shwet.job_tracker_backend.rag.ResumeVectorIndex;
import com.shwet.job_tracker_backend.repository.UserRepository;
import com.shwet.job_tracker_backend.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResumeVectorIndex resumeVectorIndex;

    @Override
    @Transactional
    public UserResponseDto registerUser(UserRequestDto requestDto) {

        String email = normalizeEmail(requestDto.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = UserMapper.toEntity(requestDto);
        user.setEmail(email);
        user.setName(requestDto.getName().trim());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));

        // A concurrent duplicate that slips past the check above is stopped by the
        // unique constraint and reported as 409 by GlobalExceptionHandler.
        return UserMapper.toResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long currentUserId, Long requestedId) {

        return UserMapper.toResponseDto(findSelf(currentUserId, requestedId));
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long currentUserId, Long requestedId, UserRequestDto requestDto) {

        User user = findSelf(currentUserId, requestedId);

        String newEmail = normalizeEmail(requestDto.getEmail());

        if (!user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        user.setName(requestDto.getName().trim());
        user.setEmail(newEmail);
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));

        return UserMapper.toResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long currentUserId, Long requestedId) {

        User user = findSelf(currentUserId, requestedId);

        // Remove the user's embeddings first (no-op when RAG is disabled/unavailable);
        // jobs and resumes are removed by the JPA cascade.
        resumeVectorIndex.deleteForUser(user.getId());

        userRepository.delete(user);
    }

    private User findSelf(Long currentUserId, Long requestedId) {

        if (currentUserId == null || !currentUserId.equals(requestedId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return userRepository.findById(requestedId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
