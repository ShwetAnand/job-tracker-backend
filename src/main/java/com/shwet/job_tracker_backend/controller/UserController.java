package com.shwet.job_tracker_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shwet.job_tracker_backend.dto.UserRequestDto;
import com.shwet.job_tracker_backend.dto.UserResponseDto;
import com.shwet.job_tracker_backend.security.CustomUserDetails;
import com.shwet.job_tracker_backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Public: registration
    @PostMapping
    public ResponseEntity<UserResponseDto> registerUser(
            @Valid @RequestBody UserRequestDto requestDto) {

        return new ResponseEntity<>(userService.registerUser(requestDto), HttpStatus.CREATED);
    }

    // ---- "me" endpoints (preferred) ----

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(userService.getUser(principal.getId(), principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateMe(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserRequestDto requestDto) {

        return ResponseEntity.ok(userService.updateUser(principal.getId(), principal.getId(), requestDto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMe(@AuthenticationPrincipal CustomUserDetails principal) {

        userService.deleteUser(principal.getId(), principal.getId());
        return ResponseEntity.ok("User Deleted Successfully");
    }

    // ---- id-based endpoints kept for frontend compatibility: only your own id works ----

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        return ResponseEntity.ok(userService.getUser(principal.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto requestDto) {

        return ResponseEntity.ok(userService.updateUser(principal.getId(), id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {

        userService.deleteUser(principal.getId(), id);
        return ResponseEntity.ok("User Deleted Successfully");
    }

    // NOTE: "GET /api/users" (list every user) was removed - it exposed all accounts' emails.
}
