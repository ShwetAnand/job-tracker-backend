package com.shwet.job_tracker_backend.service;

import com.shwet.job_tracker_backend.dto.LoginRequestDto;
import com.shwet.job_tracker_backend.dto.LoginResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto requestDto);

}