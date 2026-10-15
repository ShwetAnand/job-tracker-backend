package com.shwet.job_tracker_backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Note: there is intentionally no userId. The owner is always the authenticated user.
 * Older clients may still send "userId"; Jackson ignores unknown properties, so such
 * requests keep working but the value is never used.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequestDto {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be at most 255 characters")
    private String companyName;

    @NotBlank(message = "Role is required")
    @Size(max = 255, message = "Role must be at most 255 characters")
    private String role;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;

    @NotNull(message = "Applied date is required")
    private LocalDate appliedDate;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;

    // Job description for AI analysis
    @Size(max = 20000, message = "Job description must be at most 20000 characters")
    private String jobDescription;
}
