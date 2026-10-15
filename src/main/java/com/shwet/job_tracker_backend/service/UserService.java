package com.shwet.job_tracker_backend.service;

import com.shwet.job_tracker_backend.dto.UserRequestDto;
import com.shwet.job_tracker_backend.dto.UserResponseDto;

/**
 * Users can only see and change their own account. Methods that take both a
 * {@code currentUserId} and a {@code requestedId} answer "not found" when they differ,
 * so other accounts are indistinguishable from non-existent ones.
 */
public interface UserService {

    UserResponseDto registerUser(UserRequestDto requestDto);

    UserResponseDto getUser(Long currentUserId, Long requestedId);

    UserResponseDto updateUser(Long currentUserId, Long requestedId, UserRequestDto requestDto);

    void deleteUser(Long currentUserId, Long requestedId);
}
