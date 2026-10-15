package com.shwet.job_tracker_backend.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shwet.job_tracker_backend.dto.LoginRequestDto;
import com.shwet.job_tracker_backend.dto.LoginResponseDto;
import com.shwet.job_tracker_backend.entity.User;
import com.shwet.job_tracker_backend.repository.UserRepository;
import com.shwet.job_tracker_backend.security.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Compared against when the email is unknown so both failures cost about the same time. */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-equalisation");
    }

    @Override
    public LoginResponseDto login(LoginRequestDto requestDto) {

        String email = requestDto.getEmail().trim().toLowerCase(Locale.ROOT);

        Optional<User> found = userRepository.findByEmailIgnoreCase(email);

        boolean passwordMatches = passwordEncoder.matches(
                requestDto.getPassword(),
                found.map(User::getPassword).orElse(dummyHash));

        if (found.isEmpty() || !passwordMatches) {
            // One message for both cases: do not reveal which emails are registered.
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = found.get();

        return LoginResponseDto.builder()
                .token(jwtService.generateToken(user.getEmail()))
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
