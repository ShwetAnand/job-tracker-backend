package com.shwet.job_tracker_backend.mapper;

import com.shwet.job_tracker_backend.dto.UserRequestDto;
import com.shwet.job_tracker_backend.dto.UserResponseDto;
import com.shwet.job_tracker_backend.entity.User;

public class UserMapper {

    public static User toEntity(UserRequestDto dto) {

        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .build();
    }

    public static UserResponseDto toResponseDto(User user) {

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}